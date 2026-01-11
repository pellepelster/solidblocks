package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.model.*
import de.solidblocks.cloudinit.model.CloudInitUserData.Companion.SCRIPT_PLACEHOLDER
import de.solidblocks.cloudinit.model.CloudInitUserData.Companion.VARIABLES_PLACEHOLDER
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.hetzner.HetznerServerTestContext
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText

@ExtendWith(SolidblocksTest::class)
class CloudInitUserDataTest {

    @Test
    fun testIntegration(testContext: SolidblocksTestContext) {
        val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val volume = hetznerTestContext.createVolume()
        val sshKey = hetznerTestContext.createSSHKey()


        val randomContent = UUID.randomUUID().toString()

        val cloudInitUserData = CloudInitUserData()
        cloudInitUserData.addCommand(Mount(volume.linuxDevice, "/storage/data"))
        cloudInitUserData.addCommand(File(randomContent.toByteArray(), "/tmp/foo-bar"))

        val serverTestContext =
            hetznerTestContext.createServer(cloudInitUserData.render(), sshKey, volumes = listOf(volume.id))
        waitForSuccessfulProvisioning(serverTestContext)
        var sshContext = serverTestContext.ssh()

        sshContext.fileExists("/tmp/foo-bar") shouldBe true
        sshContext.filePermissions("/tmp/foo-bar") shouldBe "-rw-------"
        sshContext.download("/tmp/foo-bar") shouldBe randomContent.toByteArray()

        val randomUUID = UUID.randomUUID().toString()

        val randomFile = Files.createTempFile("random", ".txt").also { it.writeText(randomUUID) }
        sshContext.upload(randomFile.toAbsolutePath(), "/storage/data/${randomUUID}.txt")
        sshContext.download("/storage/data/${randomUUID}.txt") shouldBe randomUUID.toByteArray()
        hetznerTestContext.destroyServer(serverTestContext)

        val recreatedServerTestContext =
            hetznerTestContext.createServer(cloudInitUserData.render(), sshKey, volumes = listOf(volume.id))
        waitForSuccessfulProvisioning(recreatedServerTestContext)

        sshContext = recreatedServerTestContext.ssh()
        sshContext.download("/storage/data/${randomUUID}.txt") shouldBe randomUUID.toByteArray()
    }

    private fun waitForSuccessfulProvisioning(serverTestContext: HetznerServerTestContext) {
        val hostTestContext = serverTestContext.host()

        await().atMost(1, TimeUnit.MINUTES).pollInterval(ofSeconds(5)).until {
            hostTestContext.portIsOpen(22)
        }

        val cloudInitContext = serverTestContext.cloudInit()
        cloudInitContext.printOutputLogOnTestFailure()

        await().atMost(1, TimeUnit.MINUTES).pollInterval(ofSeconds(10)).until {
            cloudInitContext.isFinished()
        }
        cloudInitContext.result()?.hasErrors shouldBe false
    }

    @Test
    fun testDefaultFilePermission() {
        FilePermission().renderChmod() shouldBe "u=rw-,g=---,o=---"
    }

    @Test
    fun testPlaceHolders() {
        val template = CloudInit1::class.java.getResource("/blcks-cloud-init-bootstrap.sh.template").readText()
        template shouldContain "__CLOUD_INIT_VARIABLES__"
        template shouldContain "__CLOUD_INIT_SCRIPT__"
    }

    @Test
    fun testRender() {
        val rendered = CloudInitUserData().also {
            it.addCommand(Mount("/dev/device1", "/mount/mount1"))
        }.render()
        rendered shouldNotContain VARIABLES_PLACEHOLDER
        rendered shouldNotContain SCRIPT_PLACEHOLDER

        println("=======================================================================")
        println(rendered)
        println("=======================================================================")
    }

    @Test
    fun testFilePermission() {
        FilePermission(
            UserPermission(true, true, true),
            GroupPermission(true, false, true),
            OtherPermission(false, true, false)
        ).renderChmod() shouldBe "u=rwx,g=r-x,o=-w-"
        FilePermission(
            UserPermission(true, true, true),
            GroupPermission(true, false, true),
            OtherPermission(false, true, false)
        ).renderChmod() shouldBe "u=rwx,g=r-x,o=-w-"
        FilePermission(
            UserPermission(false, false, false),
            GroupPermission(false, false, false),
            OtherPermission(false, false, false)
        ).renderChmod() shouldBe "u=---,g=---,o=---"
    }
}