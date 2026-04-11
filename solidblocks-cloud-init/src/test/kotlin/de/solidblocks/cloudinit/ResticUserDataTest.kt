package de.solidblocks.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.generateRandomString
import de.solidblocks.restic.resticLocalAndS3Backup
import de.solidblocks.restic.resticLocalBackup
import de.solidblocks.restic.resticS3Backup
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import io.kotest.assertions.assertSoftly
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
        val sshKey = hetzner.createSSHKey()

        val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")
        val backupVolume = hetzner.createVolume("${hetzner.testId}-backup")

        val randomContent = UUID.randomUUID().toString()
        val repoPassword = UUID.randomUUID().toString()

        val backupMount = "/storage/backup"
        val localRepository = "$backupMount/repo1"
        val backupPath = "/storage/data/foo-bar"

        val userData = ShellScript()
        userData.addInlineSource(StorageLibrary)
        userData.addInlineSource(AptLibrary)
        userData.addCommand(AptLibrary.UpdateRepositories())
        userData.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))
        userData.addCommand(StorageLibrary.Mount(backupVolume.linuxDevice, backupMount))
        userData.resticLocalBackup(localRepository, repoPassword, backupPath)

        val server =
            hetzner.createServer(
                userData.render(),
                sshKey,
                volumes = listOf(dataVolume1.id, backupVolume.id),
            )
        server.waitForSuccessfulProvisioning()

        // create unique data and force a backup
        val sshContext = server.ssh()
        sshContext.command("mkdir -p /storage/data/foo-bar/").exitCode shouldBe 0
        sshContext.command("echo '$randomContent' > /storage/data/foo-bar/file.txt").exitCode shouldBe 0
        sshContext.command("systemctl start backup-storage-data-foo-bar-local").exitCode shouldBe 0

        Thread.sleep(5000)
        println(sshContext.command("journalctl -u backup-storage-data-foo-bar-local").stdout)

        // delete server and data disk, while keeping the backup disk
        hetzner.destroyServer(server)
        hetzner.destroyVolume(dataVolume1) shouldBe true

        // re-create server with new data disk
        val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")

        val userDataRestore = ShellScript()
        userDataRestore.addInlineSource(StorageLibrary)
        userDataRestore.addInlineSource(AptLibrary)
        userDataRestore.addCommand(AptLibrary.UpdateRepositories())
        userDataRestore.addCommand(StorageLibrary.Mount(dataVolume2.linuxDevice, "/storage/data"))
        userDataRestore.addCommand(StorageLibrary.Mount(backupVolume.linuxDevice, backupMount))
        userDataRestore.resticLocalBackup(localRepository, repoPassword, backupPath)

        val serverRestore =
            hetzner.createServer(
                userDataRestore.render(),
                sshKey,
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
        val sshKey = hetzner.createSSHKey()

        val randomContent = UUID.randomUUID().toString()
        val repoPassword = UUID.randomUUID().toString()

        val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")

        val userData = ShellScript()
        userData.addInlineSource(StorageLibrary)
        userData.addInlineSource(AptLibrary)
        userData.addCommand(AptLibrary.UpdateRepositories())
        userData.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))

        val s3Repository = "s3:s3.eu-central-1.amazonaws.com/$bucket/${generateRandomString(5)}"

        userData.resticS3Backup(
            s3Repository,
            repoPassword,
            getenv("AWS_ACCESS_KEY_ID"),
            getenv("AWS_SECRET_ACCESS_KEY"),
            "/data/foo-bar/",
        )

        val server =
            hetzner.createServer(
                userData.render(),
                sshKey,
                volumes = listOf(dataVolume1.id),
            )
        server.waitForSuccessfulProvisioning()

        // create unique data and force a backup
        val sshContext = server.ssh()
        sshContext.command("mkdir -p /data/foo-bar/").exitCode shouldBe 0
        sshContext.command("echo '$randomContent' > /data/foo-bar/file.txt").exitCode shouldBe 0
        sshContext.command("systemctl start backup-data-foo-bar-s3").exitCode shouldBe 0

        Thread.sleep(5000)
        println(sshContext.command("journalctl -u backup-data-foo-bar-s3").stdout)

        hetzner.destroyServer(server)

        // re-create server with new data disk
        val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")

        val userDataRestore = ShellScript()
        userDataRestore.addInlineSource(StorageLibrary)
        userDataRestore.addInlineSource(AptLibrary)
        userData.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))

        userDataRestore.addCommand(AptLibrary.UpdateRepositories())
        userDataRestore.resticS3Backup(
            s3Repository,
            repoPassword,
            getenv("AWS_ACCESS_KEY_ID"),
            getenv("AWS_SECRET_ACCESS_KEY"),
            "/data/foo-bar/",
        )

        val serverRestore =
            hetzner.createServer(
                userDataRestore.render(),
                sshKey,
                volumes = listOf(dataVolume2.id),
            )
        serverRestore.waitForSuccessfulProvisioning()

        val sshRestore = serverRestore.ssh()
        sshRestore.fileExists("/data/foo-bar/file.txt") shouldBe true
        sshRestore.download("/data/foo-bar/file.txt") shouldBe (randomContent + "\n").toByteArray()
    }

    @Test
    fun testRecoveryFromLocalAndS3(context: SolidblocksTestContext) {
        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val bucket = context.aws().createBucket()
        val sshKey = hetzner.createSSHKey()

        val randomContent1 = UUID.randomUUID().toString()
        val randomContent2 = UUID.randomUUID().toString()
        val repositoryPassword = UUID.randomUUID().toString()

        val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")
        val backupVolume1 = hetzner.createVolume("${hetzner.testId}-backup1")

        val backupMount = "/storage/backup"
        val localRepository = "$backupMount/repo1"
        val s3Repository = "s3:s3.eu-central-1.amazonaws.com/$bucket/${generateRandomString(5)}"

        val userData = ShellScript()
        userData.addInlineSource(StorageLibrary)
        userData.addInlineSource(AptLibrary)
        userData.addCommand(AptLibrary.UpdateRepositories())
        userData.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))
        userData.addCommand(StorageLibrary.Mount(backupVolume1.linuxDevice, backupMount))

        userData.resticLocalAndS3Backup(
            localRepository,
            s3Repository,
            repositoryPassword,
            getenv("AWS_ACCESS_KEY_ID"),
            getenv("AWS_SECRET_ACCESS_KEY"),
            "/storage/data/foo-bar/",
        )

        val server =
            hetzner.createServer(
                userData.render(),
                sshKey,
                volumes = listOf(dataVolume1.id, backupVolume1.id),
            )
        server.waitForSuccessfulProvisioning()

        // create unique data and force a backup
        val sshContext = server.ssh()
        sshContext.command("mkdir -p /storage/data/foo-bar/").exitCode shouldBe 0

        /** trigger local backup */
        sshContext.command("echo '$randomContent1' > /storage/data/foo-bar/file.txt").exitCode shouldBe
            0
        sshContext.command("systemctl start backup-storage-data-foo-bar-local").exitCode shouldBe 0
        Thread.sleep(5000)
        println(sshContext.command("journalctl -u backup-storage-data-foo-bar-local").stdout)

        /** trigger s3 backup with new content */
        sshContext.command("echo '$randomContent2' > /storage/data/foo-bar/file.txt").exitCode shouldBe
            0
        sshContext.command("systemctl start backup-storage-data-foo-bar-s3").exitCode shouldBe 0
        Thread.sleep(5000)
        println(sshContext.command("journalctl -u backup-storage-data-foo-bar-s3").stdout)

        /** re-create server with new data disk */
        hetzner.destroyServer(server)
        hetzner.destroyVolume(dataVolume1)
        val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")

        val userDataAfterDataVolumeDelete = ShellScript()
        userDataAfterDataVolumeDelete.addInlineSource(StorageLibrary)
        userDataAfterDataVolumeDelete.addInlineSource(AptLibrary)
        userDataAfterDataVolumeDelete.addCommand(
            StorageLibrary.Mount(dataVolume2.linuxDevice, "/storage/data"),
        )
        userDataAfterDataVolumeDelete.addCommand(
            StorageLibrary.Mount(backupVolume1.linuxDevice, backupMount),
        )

        userDataAfterDataVolumeDelete.addCommand(AptLibrary.UpdateRepositories())
        userDataAfterDataVolumeDelete.resticLocalAndS3Backup(
            localRepository,
            s3Repository,
            repositoryPassword,
            getenv("AWS_ACCESS_KEY_ID"),
            getenv("AWS_SECRET_ACCESS_KEY"),
            "/storage/data/foo-bar/",
        )

        val serverRestore =
            hetzner.createServer(
                userDataAfterDataVolumeDelete.render(),
                sshKey,
                volumes = listOf(dataVolume2.id, backupVolume1.id),
            )
        serverRestore.waitForSuccessfulProvisioning()

        /** after provisioning data should be restored from local backup */
        assertSoftly(serverRestore.ssh()) {
            it.fileExists("/storage/data/foo-bar/file.txt") shouldBe true
            it.download("/storage/data/foo-bar/file.txt") shouldBe (randomContent1 + "\n").toByteArray()
        }

        /** re-create server with new data and backup disk */
        val backupVolume2 = hetzner.createVolume("${hetzner.testId}-backup2")
        val dataVolume3 = hetzner.createVolume("${hetzner.testId}-data3")

        hetzner.destroyServer(serverRestore)
        hetzner.destroyVolume(backupVolume1)
        hetzner.destroyVolume(dataVolume2)

        val userDataAfterBackupVolumeDelete = ShellScript()
        userDataAfterBackupVolumeDelete.addInlineSource(StorageLibrary)
        userDataAfterBackupVolumeDelete.addInlineSource(AptLibrary)
        userDataAfterBackupVolumeDelete.addCommand(
            StorageLibrary.Mount(dataVolume3.linuxDevice, "/storage/data"),
        )
        userDataAfterBackupVolumeDelete.addCommand(
            StorageLibrary.Mount(backupVolume2.linuxDevice, backupMount),
        )

        userDataAfterBackupVolumeDelete.addCommand(AptLibrary.UpdateRepositories())
        userDataAfterBackupVolumeDelete.resticLocalAndS3Backup(
            localRepository,
            s3Repository,
            repositoryPassword,
            getenv("AWS_ACCESS_KEY_ID"),
            getenv("AWS_SECRET_ACCESS_KEY"),
            "/storage/data/foo-bar/",
        )

        val serverAfterBackupVolumeDelete =
            hetzner.createServer(
                userDataAfterBackupVolumeDelete.render(),
                sshKey,
                volumes = listOf(dataVolume3.id, backupVolume2.id),
            )
        serverAfterBackupVolumeDelete.waitForSuccessfulProvisioning()

        /** after provisioning data should be restored from s3 backup */
        assertSoftly(serverAfterBackupVolumeDelete.ssh()) {
            it.fileExists("/storage/data/foo-bar/file.txt") shouldBe true
            it.download("/storage/data/foo-bar/file.txt") shouldBe (randomContent2 + "\n").toByteArray()
        }
    }
}
