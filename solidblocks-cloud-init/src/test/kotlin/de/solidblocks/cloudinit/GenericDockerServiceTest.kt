package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.GenericDockerServiceUserData
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.concurrent.TimeUnit

@ExtendWith(SolidblocksTest::class)
class GenericDockerServiceTest {
    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
    fun testIntegration(testContext: SolidblocksTestContext) {
        val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val dataVolume = hetznerTestContext.createVolume("${testContext.testId}-data")
        val backupVolume = hetznerTestContext.createVolume("${testContext.testId}-backup")
        val sshKey = hetznerTestContext.createSSHKey()

        val backupConfiguration = BackupConfiguration("very-secret", LocalBackupTarget(backupVolume.linuxDevice))

        val userData =
            GenericDockerServiceUserData(
                testContext.testId,
                dataVolume.linuxDevice,
                backupConfiguration,
                "nginx",
                mapOf(80 to 80),
                null,
            )

        val serverTestContext =
            hetznerTestContext.createServer(
                userData.render(),
                sshKey,
                volumes = listOf(dataVolume.id, backupVolume.id),
            )

        serverTestContext.waitForSuccessfulProvisioning()

        Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(5)).until {
            serverTestContext.host().portIsOpen(80)
        }
    }
}
