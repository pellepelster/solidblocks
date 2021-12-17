package de.solidblocks.vault

import de.solidblocks.test.SolidblocksLocalEnv
import de.solidblocks.test.SolidblocksLocalEnvExtension
import de.solidblocks.vault.VaultConstants.ROOT_TOKEN_KEY
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExtendWith(SolidblocksLocalEnvExtension::class)
class VaultCertificateManagerTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testIssueCertificates(solidblocksLocalEnv: SolidblocksLocalEnv) {
        val rootToken = solidblocksLocalEnv.environmentConfiguration.getConfigValue(ROOT_TOKEN_KEY)

        val vaultManager = VaultCertificateManager(
            address = solidblocksLocalEnv.vaultAddress,
            token = rootToken,
            cloud = solidblocksLocalEnv.cloud,
            environment = solidblocksLocalEnv.environment,
            service = "service1",
            minCertificateLifetime = Duration.Companion.days(300)
        )

        val firstCertificate = await untilNotNull { vaultManager.certificate }
        await untilCallTo {
            vaultManager.certificate?.public?.serialNumber
        } matches { serial ->
            serial != firstCertificate.public.serialNumber
        }

        assertThat(firstCertificate.public.subjectX500Principal.name).isEqualTo("CN=service1")
    }
}
