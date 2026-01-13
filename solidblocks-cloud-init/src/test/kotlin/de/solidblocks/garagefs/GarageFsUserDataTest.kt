package de.solidblocks.garagefs

import de.solidblocks.cloudinit.waitForSuccessfulProvisioning
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.TimeUnit

@ExtendWith(SolidblocksTest::class)
class GarageFsUserDataTest {

    @Test
    fun testIntegration(testContext: SolidblocksTestContext) {
        val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val volume = hetznerTestContext.createVolume()
        val sshKey = hetznerTestContext.createSSHKey()

        val userData = GarageFsUserData(
            volume.linuxDevice,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "yolo.de"
        )

        val serverTestContext =
            hetznerTestContext.createServer(userData.render(), sshKey, volumes = listOf(volume.id))

        serverTestContext.waitForSuccessfulProvisioning()

        await().atMost(1, TimeUnit.MINUTES).pollInterval(ofSeconds(5)).until {
            serverTestContext.host().portIsOpen(80)
        }
    }

    @Test
    fun testRender() {
        println(
            GarageFsUserData(
                "/dev/sdb",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "yolo.de"
            ).render()
        )
    }
}