package de.solidblocks.vault

import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
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

@ExtendWith(DevelopmentEnvironmentExtension::class)
class VaultCertificateManagerTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testIssueServiceCertificate(developmentEnvironment: DevelopmentEnvironment) {
        val rootToken = developmentEnvironment.environmentModel.getConfigValue(ROOT_TOKEN_KEY)

        val vaultManager = ServiceVaultCertificateManager(
            address = developmentEnvironment.vaultAddress,
            token = rootToken,
            developmentEnvironment.reference.toService("service1"),
            "local.test",
            minCertificateLifetime = Duration.days(300),
            checkInterval = Duration.seconds(2)
        )

        val firstCertificate = await untilNotNull { vaultManager.certificate }
        await untilCallTo {
            vaultManager.certificate?.public?.serialNumber
        } matches { serial ->
            serial != firstCertificate.public.serialNumber
        }

        assertThat(firstCertificate.public.subjectX500Principal.name).isEqualTo("CN=service1.tenant1.dev.local.test")
        assertThat(firstCertificate.public.subjectAlternativeNames.map { it[1] }).contains("127.0.0.1")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testIssueEnvironmentCertificate(developmentEnvironment: DevelopmentEnvironment) {
        val rootToken = developmentEnvironment.environmentModel.getConfigValue(ROOT_TOKEN_KEY)

        val vaultManager = EnvironmentVaultCertificateManager(
            address = developmentEnvironment.vaultAddress,
            token = rootToken,
            developmentEnvironment.reference.toEnvironmentService("ingress"),
            "local.test",
            minCertificateLifetime = Duration.days(300),
            checkInterval = Duration.seconds(2)
        )

        val firstCertificate = await untilNotNull { vaultManager.certificate }
        await untilCallTo {
            vaultManager.certificate?.public?.serialNumber
        } matches { serial ->
            serial != firstCertificate.public.serialNumber
        }

        assertThat(firstCertificate.public.subjectX500Principal.name).isEqualTo("CN=ingress.dev.local.test")
        assertThat(firstCertificate.public.subjectAlternativeNames.map { it[1] }).contains("127.0.0.1")
    }
}
