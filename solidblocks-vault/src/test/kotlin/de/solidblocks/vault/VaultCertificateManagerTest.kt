package de.solidblocks.vault

import de.solidblocks.base.BaseConstants.serversDomain
import de.solidblocks.test.IntegrationTestEnvironment
import de.solidblocks.test.IntegrationTestExtension
import de.solidblocks.vault.VaultConstants.environmentServerPkiMountName
import de.solidblocks.vault.VaultConstants.tenantServerPkiMountName
import de.solidblocks.vault.model.VaultCertificate
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExtendWith(IntegrationTestExtension::class)
class VaultCertificateManagerTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testIssueServiceCertificate(integrationTestEnvironment: IntegrationTestEnvironment) {
        val reference = integrationTestEnvironment.reference.toService("service1")

        val vaultManager = VaultCertificateManager(
            address = integrationTestEnvironment.vaultAddress,
            token = integrationTestEnvironment.vaultRootToken,
            pkiMount = tenantServerPkiMountName(reference),
            commonName = serversDomain(reference, "local.test"),
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

        val certificateCallback = CompletableFuture<VaultCertificate>()

        VaultCertificateManager(
            address = integrationTestEnvironment.vaultAddress,
            token = integrationTestEnvironment.vaultRootToken,
            pkiMount = tenantServerPkiMountName(reference),
            commonName = serversDomain(reference, "local.test"),
            minCertificateLifetime = Duration.days(300),
            checkInterval = Duration.seconds(2)
        ) {
            certificateCallback.complete(it)
        }

        await until {
            certificateCallback.isDone
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testIssueEnvironmentCertificate(integrationTestEnvironment: IntegrationTestEnvironment) {
        val reference = integrationTestEnvironment.reference.toEnvironmentService("ingress")

        val vaultManager = VaultCertificateManager(
            address = integrationTestEnvironment.vaultAddress,
            token = integrationTestEnvironment.vaultRootToken,
            pkiMount = environmentServerPkiMountName(reference),
            commonName = serversDomain(reference, "local.test"),
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
