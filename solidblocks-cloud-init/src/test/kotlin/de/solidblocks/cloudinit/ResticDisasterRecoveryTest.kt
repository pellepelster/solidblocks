package de.solidblocks.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.generateRandomString
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.StorageLibrary
import io.kotest.matchers.shouldBe
import java.lang.System.getenv
import java.util.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class ResticDisasterRecoveryTest {

  @Test
  fun testRecoveryFromLocal(context: SolidblocksTestContext) {
    val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN").toString())
    val sshKey = hetzner.createSSHKey()

    val dataVolume1 = hetzner.createVolume("${hetzner.testId}-data1")
    val backupVolume = hetzner.createVolume("${hetzner.testId}-backup")

    val randomContent = UUID.randomUUID().toString()
    val repoPassword = UUID.randomUUID().toString()
    val backupMount = "/storage/backup"
    val name = "repo1"
    val localRepositoryPath = "/storage/backup/$name"
    val backupPath = "/storage/data/foo-bar"

    val userData = CloudInitUserData()
    userData.addSources(StorageLibrary.source())
    userData.addSources(AptLibrary.source())
    userData.addCommand(AptLibrary.UpdateRepositories())
    userData.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))
    userData.addCommand(StorageLibrary.Mount(backupVolume.linuxDevice, backupMount))
    userData.resticLocalBackup(name, localRepositoryPath, repoPassword, backupPath)

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
    sshContext.command("systemctl start backup-$name-local").exitCode shouldBe 0

    Thread.sleep(5000)
    println(sshContext.command("journalctl -u backup-$name-local").stdout)

    // delete server and data disk, while keeping the backup disk
    hetzner.destroyServer(server)
    hetzner.destroyVolume(dataVolume1) shouldBe true

    // re-create server with new data disk
    val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data2")

    val userDataRestore = CloudInitUserData()
    userDataRestore.addSources(StorageLibrary.source())
    userDataRestore.addSources(AptLibrary.source())
    userDataRestore.addCommand(AptLibrary.UpdateRepositories())
    userDataRestore.addCommand(StorageLibrary.Mount(dataVolume2.linuxDevice, "/storage/data"))
    userDataRestore.addCommand(StorageLibrary.Mount(backupVolume.linuxDevice, backupMount))
    userDataRestore.resticLocalBackup(name, localRepositoryPath, repoPassword, backupPath)

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
    val name = "repo1"
    val repoPath = generateRandomString(5)

    val userData = CloudInitUserData()
    userData.addSources(StorageLibrary.source())
    userData.addSources(AptLibrary.source())
    userData.addCommand(AptLibrary.UpdateRepositories())
    userData.resticS3Backup(
        name,
        repoPassword,
        repoPath,
        bucket,
        "eu-central-1",
        getenv("AWS_ACCESS_KEY_ID"),
        getenv("AWS_SECRET_ACCESS_KEY"),
        "/data/foo-bar/",
    )

    val server =
        hetzner.createServer(
            userData.render(),
            sshKey,
        )
    server.waitForSuccessfulProvisioning()

    // create unique data and force a backup
    val sshContext = server.ssh()
    sshContext.command("mkdir -p /data/foo-bar/").exitCode shouldBe 0
    sshContext.command("echo '$randomContent' > /data/foo-bar/file.txt").exitCode shouldBe 0
    sshContext.command("systemctl start backup-$name-s3").exitCode shouldBe 0

    Thread.sleep(5000)
    println(sshContext.command("journalctl -u backup-$name-s3").stdout)

    hetzner.destroyServer(server)

    // re-create server with new data disk
    val dataVolume2 = hetzner.createVolume("${hetzner.testId}-data1")

    val userDataRestore = CloudInitUserData()
    userDataRestore.addSources(StorageLibrary.source())
    userDataRestore.addSources(AptLibrary.source())
    userDataRestore.addCommand(AptLibrary.UpdateRepositories())
    userDataRestore.resticS3Backup(
        name,
        repoPassword,
        repoPath,
        bucket,
        "eu-central-1",
        getenv("AWS_ACCESS_KEY_ID"),
        getenv("AWS_SECRET_ACCESS_KEY"),
        "/data/foo-bar/",
    )

    val serverRestore =
        hetzner.createServer(
            userDataRestore.render(),
            sshKey,
        )
    serverRestore.waitForSuccessfulProvisioning()

    val sshRestore = serverRestore.ssh()
    sshRestore.fileExists("/data/foo-bar/file.txt") shouldBe true
    sshRestore.download("/data/foo-bar/file.txt") shouldBe (randomContent + "\n").toByteArray()
  }
}
