package de.solidblocks.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.generateRandomString
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.System.getenv
import java.util.*

@ExtendWith(SolidblocksTest::class)
class ResticUserDataTest {
    @Test
    fun testRecoveryFromLocal(context: SolidblocksTestContext) {
        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")
        val backupVolume = hetzner.createVolume("${hetzner.testId}-backup")

        val randomContent = UUID.randomUUID().toString()
        val repoPassword = UUID.randomUUID().toString()

        val backupPath = "/storage/data/foo-bar"

        val shellScript = ShellScript()
        shellScript.addLibrary(StorageLibrary)
        shellScript.addLibrary(AptLibrary)
        shellScript.addCommand(AptLibrary.UpdateRepositories())
        shellScript.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))

        val backupConfiguration = BackupConfiguration(repoPassword, LocalBackupTarget(backupVolume.linuxDevice))
        shellScript.resticBackup("repo1", backupConfiguration, backupPath)

        val server =
            hetzner.createServer(
                shellScript.render(),
                volumes = listOf(dataVolume1.id, backupVolume.id),
            )
        server.waitForSuccessfulProvisioning()

        // create unique data and force a backup
        val sshContext = server.ssh()
        sshContext.command("mkdir -p /storage/data/foo-bar/").exitCode shouldBe 0
        sshContext.command("echo '$randomContent' > /storage/data/foo-bar/file.txt").exitCode shouldBe 0
        sshContext.command("systemctl start repo1-backup-storage-data-foo-bar").exitCode shouldBe 0

        Thread.sleep(5000)
        println(sshContext.command("journalctl -u backup-storage-data-foo-bar").stdout)

        // delete server and data disk, while keeping the backup disk
        hetzner.destroyServer(server)
        hetzner.destroyVolume(dataVolume1) shouldBe true

        // re-create server with new data disk
        val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")

        val userDataRestore = ShellScript()
        userDataRestore.addLibrary(StorageLibrary)
        userDataRestore.addLibrary(AptLibrary)
        userDataRestore.addCommand(AptLibrary.UpdateRepositories())
        userDataRestore.addCommand(StorageLibrary.Mount(dataVolume2.linuxDevice, "/storage/data"))
        userDataRestore.resticBackup("repo1", backupConfiguration, backupPath)

        val serverRestore =
            hetzner.createServer(
                userDataRestore.render(),
                volumes = listOf(dataVolume2.id, backupVolume.id),
            )
        serverRestore.waitForSuccessfulProvisioning()

        val sshRestore = serverRestore.ssh()
        sshRestore.fileExists("/storage/data/foo-bar/file.txt") shouldBe true
        sshRestore.download("/storage/data/foo-bar/file.txt") shouldBe
            (randomContent + "\n").toByteArray()
    }

    @Test
    fun testRecoveryFromS3(context: SolidblocksTestContext) {
        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val bucket = context.aws().createBucket()

        val randomContent = UUID.randomUUID().toString()
        val repoPassword = UUID.randomUUID().toString()

        val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")

        val backupConfiguration = BackupConfiguration(
            repoPassword,
            S3BackupTarget(
                bucket,
                getenv("AWS_ACCESS_KEY_ID"),
                getenv("AWS_SECRET_ACCESS_KEY"),
            ),
        )

        val shellScript = ShellScript()
        shellScript.addLibrary(StorageLibrary)
        shellScript.addLibrary(AptLibrary)
        shellScript.addCommand(AptLibrary.UpdateRepositories())
        shellScript.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))

        val s3Repository = "s3:s3.eu-central-1.amazonaws.com/$bucket/${generateRandomString(5)}"

        shellScript.resticBackup(
            "repo1",
            backupConfiguration,
            "/data/foo-bar/",
        )

        val server =
            hetzner.createServer(
                shellScript.render(),
                volumes = listOf(dataVolume1.id),
            )
        server.waitForSuccessfulProvisioning()

        // create unique data and force a backup
        val sshContext = server.ssh()
        sshContext.command("mkdir -p /data/foo-bar/").exitCode shouldBe 0
        sshContext.command("echo '$randomContent' > /data/foo-bar/file.txt").exitCode shouldBe 0
        sshContext.command("systemctl start repo1-backup-data-foo-bar").exitCode shouldBe 0

        Thread.sleep(5000)
        println(sshContext.command("journalctl -u backup-data-foo-bar").stdout)

        hetzner.destroyServer(server)

        // re-create server with new data disk
        val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")

        val shellSCriptRestore = ShellScript()
        shellSCriptRestore.addLibrary(StorageLibrary)
        shellSCriptRestore.addLibrary(AptLibrary)
        shellScript.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))

        shellSCriptRestore.addCommand(AptLibrary.UpdateRepositories())
        shellSCriptRestore.resticBackup(
            "repo1",
            backupConfiguration,
            "/data/foo-bar/",
        )

        val serverRestore =
            hetzner.createServer(
                shellSCriptRestore.render(),
                volumes = listOf(dataVolume2.id),
            )
        serverRestore.waitForSuccessfulProvisioning()

        val sshRestore = serverRestore.ssh()
        sshRestore.fileExists("/data/foo-bar/file.txt") shouldBe true
        sshRestore.download("/data/foo-bar/file.txt") shouldBe (randomContent + "\n").toByteArray()
    }
}
