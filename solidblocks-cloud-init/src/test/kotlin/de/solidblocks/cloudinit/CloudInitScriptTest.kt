package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.model.*
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExtendWith(SolidblocksTest::class)
class CloudInitTest {

    @Test
    fun testIntegration(context: SolidblocksTestContext) {

        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val cloudInit = CloudInit()
        cloudInit.mounts.add(Mount("dd", "dd"))
        cloudInit.files.add(File("yolo".toByteArray(), "/tmp/yolo"))

        val server = hetzner.createServer()

        val cloudInit1 = server.cloudInit()

        await().atMost(1, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(10)).until {
            cloudInit1.isFinished()
        }
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