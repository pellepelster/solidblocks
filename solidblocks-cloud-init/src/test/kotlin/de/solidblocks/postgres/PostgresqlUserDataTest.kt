package de.solidblocks.postgres

import de.solidblocks.cloudinit.waitForSuccessfulProvisioning
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.postgresql.PostgresqlUserData
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.TimeUnit
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class PostgresqlUserDataTest {

  @Test
  @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
  fun testFlow(testContext: SolidblocksTestContext) {
    val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

    val dataVolume = hetznerTestContext.createVolume("${hetznerTestContext.testId}-data")
    val backupVolume = hetznerTestContext.createVolume("${hetznerTestContext.testId}-backup")
    val sshKey = hetznerTestContext.createSSHKey()

    val userData =
        PostgresqlUserData(
            dataVolume.linuxDevice,
            backupVolume.linuxDevice,
            "instance1",
            "very-secret",
        )

    val serverTestContext =
        hetznerTestContext.createServer(
            userData.render(),
            sshKey,
            volumes = listOf(backupVolume.id, dataVolume.id),
        )

    serverTestContext.waitForSuccessfulProvisioning()

    await().atMost(5, TimeUnit.MINUTES).pollInterval(ofSeconds(5)).until {
      serverTestContext.host().portIsOpen(5432)
    }
  }
}
