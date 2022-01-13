package de.solidblocks.vault

import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import de.solidblocks.vault.VaultConstants.environmentServerPkiMountName
import de.solidblocks.vault.VaultConstants.tenantServerPkiMountName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.ExperimentalTime

@ExtendWith(DevelopmentEnvironmentExtension::class)
class VaultCaCertificateManagerTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGetTenantCaCertificate(developmentEnvironment: DevelopmentEnvironment) {
        val reference = developmentEnvironment.reference.toService("service1")

        val caCertificateManager = VaultCaCertificateManager(
            address = developmentEnvironment.vaultAddress,
            token = developmentEnvironment.vaultRootToken,
            pkiMount = tenantServerPkiMountName(reference)
        )

        val caCertificate = caCertificateManager.waitForCaCertificate()
        assertThat(caCertificate.caCertificate.subjectX500Principal.name).isEqualTo("CN=local-dev-tenant1-pki-server root")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testGetEnvironmentCaCertificate(developmentEnvironment: DevelopmentEnvironment) {
        val reference = developmentEnvironment.reference.toService("service1")

        val caCertificateManager = VaultCaCertificateManager(
            address = developmentEnvironment.vaultAddress,
            token = developmentEnvironment.vaultRootToken,
            pkiMount = environmentServerPkiMountName(reference)
        )

        val caCertificate = caCertificateManager.waitForCaCertificate()
        assertThat(caCertificate.caCertificate.subjectX500Principal.name).isEqualTo("CN=local-dev-pki-server root")
    }
}
