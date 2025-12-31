package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.model.*
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(SolidblocksTest::class)
class CloudInitScriptTest {

    @Test
    fun testIntegration(testContext: SolidblocksTestContext) {
        val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val content = UUID.randomUUID().toString()
        val cloudInitScript = CloudInitScript()
        //cloudInitScript.mounts.add(Mount("dd", "dd"))

        cloudInitScript.files.add(File(content.toByteArray(), "/tmp/foo-bar"))

        val serverTestContext = hetznerTestContext.createServer(cloudInitScript.render())
        val hostTestContext = serverTestContext.host()

        await().atMost(1, TimeUnit.MINUTES).pollInterval(ofSeconds(5)).until {
            hostTestContext.portIsOpen(22)
        }

        val cloudInitContext = serverTestContext.cloudInit()

        await().atMost(3, TimeUnit.MINUTES).pollInterval(ofSeconds(10)).until {
            try {
                cloudInitContext.isFinished()
            } catch (e: Exception) {
                false
            }
        }

        cloudInitContext.result()?.hasErrors shouldBe false

        val sshContext = serverTestContext.ssh()

        sshContext.fileExists("/tmp/foo-bar") shouldBe true
        sshContext.filePermissions("/tmp/foo-bar") shouldBe "-rw-------"
        sshContext.download("/tmp/foo-bar") shouldBe content.toByteArray()
    }

    @Test
    fun testDefaultFilePermission() {
        FilePermission().renderChmod() shouldBe "u=rw-,g=---,o=---"
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