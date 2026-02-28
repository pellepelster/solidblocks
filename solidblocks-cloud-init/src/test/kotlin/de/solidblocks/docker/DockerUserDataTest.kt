package de.solidblocks.docker

import de.solidblocks.cloudinit.waitForSuccessfulProvisioning
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.TimeUnit
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class DockerUserDataTest {

  @Test
  @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
  fun testIntegration(testContext: SolidblocksTestContext) {
    val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

    val volume = hetznerTestContext.createVolume()
    val sshKey = hetznerTestContext.createSSHKey()

    val userData =
        DockerUserData(
            volume.linuxDevice,
            "yolo.de",
            "nginx",
            80,
        )

    val serverTestContext =
        hetznerTestContext.createServer(userData.render(), sshKey, volumes = listOf(volume.id))

    serverTestContext.waitForSuccessfulProvisioning()

    await().atMost(5, TimeUnit.MINUTES).pollInterval(ofSeconds(5)).until {
      serverTestContext.host().portIsOpen(80)
    }
  }
}
