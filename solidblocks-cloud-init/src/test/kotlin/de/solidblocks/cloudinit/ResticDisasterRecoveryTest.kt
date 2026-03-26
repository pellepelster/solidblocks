package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.model.CloudInitUserData
import de.solidblocks.cloudinit.model.installRestic
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.StorageLibrary
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class ResticDisasterRecoveryTest {

  @Test
  fun testDisasterRecoveryFromLocalDisk(testContext: SolidblocksTestContext) {
    val context = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())
    val sshKey = context.createSSHKey()

    val dataVolume1 = context.createVolume("${context.testId}-data2")
    val backupVolume = context.createVolume("${context.testId}-backup")

    val randomContent = UUID.randomUUID().toString()
    val repoPassword = UUID.randomUUID().toString()
    val backupMount = "/storage/backup"
    val repoName = "repo1"
    val localRepositoryPath = "/storage/backup/$repoName"
    val backupPath = "/storage/data/foo-bar"

    val userData = CloudInitUserData()
    userData.addSources(StorageLibrary.source())
    userData.addSources(AptLibrary.source())
    userData.addCommand(AptLibrary.UpdateRepositories())
    userData.addCommand(StorageLibrary.Mount(dataVolume1.linuxDevice, "/storage/data"))
    userData.addCommand(StorageLibrary.Mount(backupVolume.linuxDevice, backupMount))
    userData.installRestic(repoName, localRepositoryPath, repoPassword, backupPath)

    val server =
        context.createServer(
            userData.render(),
            sshKey,
            volumes = listOf(dataVolume1.id, backupVolume.id),
        )
    server.waitForSuccessfulProvisioning()

    // create unique data and force a backup
    val sshContext = server.ssh()
    sshContext.command("mkdir -p /storage/data/foo-bar/").exitCode shouldBe 0
    sshContext.command("echo '$randomContent' > /storage/data/foo-bar/file.txt").exitCode shouldBe 0
    sshContext.command("systemctl start backup-$repoName").exitCode shouldBe 0

    Thread.sleep(5000)
    println(sshContext.command("journalctl -u backup-$repoName").stdout)

    // delete server and data disk, while keeping the backup disk
    context.destroyServer(server)
    context.destroyVolume(dataVolume1) shouldBe true

    // re-create server with new data disk
    val dataVolume2 = context.createVolume("${context.testId}-data1")

    val userDataRestore = CloudInitUserData()
    userDataRestore.addSources(StorageLibrary.source())
    userDataRestore.addSources(AptLibrary.source())
    userDataRestore.addCommand(AptLibrary.UpdateRepositories())
    userDataRestore.addCommand(StorageLibrary.Mount(dataVolume2.linuxDevice, "/storage/data"))
    userDataRestore.addCommand(StorageLibrary.Mount(backupVolume.linuxDevice, backupMount))
    userDataRestore.installRestic(repoName, localRepositoryPath, repoPassword, backupPath)

    val serverRestore =
        context.createServer(
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
}
