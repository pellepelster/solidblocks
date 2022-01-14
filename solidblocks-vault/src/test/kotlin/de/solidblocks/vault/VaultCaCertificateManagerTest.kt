package de.solidblocks.vault

import de.solidblocks.test.IntegrationTestEnvironment
import de.solidblocks.test.IntegrationTestExtension
import de.solidblocks.vault.VaultConstants.environmentServerPkiMountName
import de.solidblocks.vault.VaultConstants.tenantServerPkiMountName
import de.solidblocks.vault.model.VaultCaCertificate
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CompletableFuture
import kotlin.time.ExperimentalTime

@ExtendWith(IntegrationTestExtension::class)
class VaultCaCertificateManagerTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGetTenantCaCertificate(integrationTestEnvironment: IntegrationTestEnvironment) {
        val reference = integrationTestEnvironment.reference.toService("service1")

        val caCertificateManager = VaultCaCertificateManager(
            address = integrationTestEnvironment.vaultAddress,
            token = integrationTestEnvironment.vaultRootToken,
            pkiMount = tenantServerPkiMountName(reference)
        )

        val caCertificate = caCertificateManager.waitForCaCertificate()
        assertThat(caCertificate.caCertificate.subjectX500Principal.name).isEqualTo("CN=local-dev-tenant1-pki-server root")

        val caCertificateCallback = CompletableFuture<VaultCaCertificate>()
        VaultCaCertificateManager(
            address = integrationTestEnvironment.vaultAddress,
            token = integrationTestEnvironment.vaultRootToken,
            pkiMount = tenantServerPkiMountName(reference)
        ) {
            caCertificateCallback.complete(it)
        }

        await until {
            caCertificateCallback.isDone
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGetEnvironmentCaCertificate(integrationTestEnvironment: IntegrationTestEnvironment) {
        val reference = integrationTestEnvironment.reference.toService("service1")

        val caCertificateManager = VaultCaCertificateManager(
            address = integrationTestEnvironment.vaultAddress,
            token = integrationTestEnvironment.vaultRootToken,
            pkiMount = environmentServerPkiMountName(reference)
        )

        val caCertificate = caCertificateManager.waitForCaCertificate()
        assertThat(caCertificate.caCertificate.subjectX500Principal.name).isEqualTo("CN=local-dev-pki-server root")
    }
}
