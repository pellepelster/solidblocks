package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.model.*
import de.solidblocks.cloudinit.model.CloudInitUserData.Companion.SCRIPT_PLACEHOLDER
import de.solidblocks.cloudinit.model.CloudInitUserData.Companion.VARIABLES_PLACEHOLDER
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.hetzner.HetznerServerTestContext
import de.solidblocks.shell.StorageLibrary
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class CloudInitUserDataTest {

  @Test
  @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
  fun testIntegration(testContext: SolidblocksTestContext) {
    val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

    val volume = hetznerTestContext.createVolume()
    val sshKey = hetznerTestContext.createSSHKey()

    val randomContent = UUID.randomUUID().toString()

    val cloudInitUserData = CloudInitUserData()
    cloudInitUserData.addCommand(StorageLibrary.Mount(volume.linuxDevice, "/storage/data"))
    cloudInitUserData.addCommand(WriteFile(randomContent.toByteArray(), "/tmp/foo-bar"))

    val serverTestContext =
        hetznerTestContext.createServer(
            cloudInitUserData.render(),
            sshKey,
            volumes = listOf(volume.id),
        )
    serverTestContext.waitForSuccessfulProvisioning()
    var sshContext = serverTestContext.ssh()

    sshContext.fileExists("/tmp/foo-bar") shouldBe true
    sshContext.filePermissions("/tmp/foo-bar") shouldBe "-rw-------"
    sshContext.download("/tmp/foo-bar") shouldBe randomContent.toByteArray()

    val randomUUID = UUID.randomUUID().toString()

    val randomFile = Files.createTempFile("random", ".txt").also { it.writeText(randomUUID) }
    sshContext.upload(randomFile.toAbsolutePath(), "/storage/data/$randomUUID.txt")
    sshContext.download("/storage/data/$randomUUID.txt") shouldBe randomUUID.toByteArray()
    hetznerTestContext.destroyServer(serverTestContext)

    val recreatedServerTestContext =
        hetznerTestContext.createServer(
            cloudInitUserData.render(),
            sshKey,
            volumes = listOf(volume.id),
        )
    recreatedServerTestContext.waitForSuccessfulProvisioning()

    sshContext = recreatedServerTestContext.ssh()
    sshContext.download("/storage/data/$randomUUID.txt") shouldBe randomUUID.toByteArray()
  }

  @Test
  fun testDefaultFilePermission() {
    FilePermissions().renderChmod() shouldBe "u=rw-,g=---,o=---"
  }

  @Test
  fun testPlaceHolders() {
    val template =
        CloudInitUserData::class
            .java
            .getResource("/blcks-cloud-init-bootstrap.sh.template")
            .readText()
    template shouldContain "__CLOUD_INIT_VARIABLES__"
    template shouldContain "__CLOUD_INIT_SCRIPT__"
  }

  @Test
  fun testRender() {
    val rendered =
        CloudInitUserData()
            .also { it.addCommand(StorageLibrary.Mount("/dev/device1", "/mount/mount1")) }
            .render()
    rendered shouldNotContain VARIABLES_PLACEHOLDER
    rendered shouldNotContain SCRIPT_PLACEHOLDER

    println("=======================================================================")
    println(rendered)
    println("=======================================================================")
  }

  @Test
  fun testFilePermission() {
    FilePermissions(
            UserPermission(true, true, true),
            GroupPermission(true, false, true),
            OtherPermission(false, true, false),
        )
        .renderChmod() shouldBe "u=rwx,g=r-x,o=-w-"
    FilePermissions(
            UserPermission(true, true, true),
            GroupPermission(true, false, true),
            OtherPermission(false, true, false),
        )
        .renderChmod() shouldBe "u=rwx,g=r-x,o=-w-"
    FilePermissions(
            UserPermission(false, false, false),
            GroupPermission(false, false, false),
            OtherPermission(false, false, false),
        )
        .renderChmod() shouldBe "u=---,g=---,o=---"
  }
}

fun HetznerServerTestContext.waitForSuccessfulProvisioning() {
  val hostTestContext = this.host()

  await().atMost(1, TimeUnit.MINUTES).pollInterval(ofSeconds(5)).until {
    hostTestContext.portIsOpen(22)
  }

  val cloudInitContext = this.cloudInit()
  cloudInitContext.printOutputLogOnTestFailure()

  await().atMost(2, TimeUnit.MINUTES).pollInterval(ofSeconds(10)).until {
    cloudInitContext.isFinished()
  }
  cloudInitContext.result()?.hasErrors shouldBe false
}
