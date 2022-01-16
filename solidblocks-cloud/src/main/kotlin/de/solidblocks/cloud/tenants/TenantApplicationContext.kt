package de.solidblocks.cloud.tenants

import de.solidblocks.base.BaseConstants.tenantHostFQDN
import de.solidblocks.base.resources.TenantResource
import de.solidblocks.cloud.model.ModelConstants.vaultAddress
import de.solidblocks.cloud.model.TenantRepository
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants.clientFQDN
import de.solidblocks.vault.VaultConstants.tenantClientPkiMountName
import de.solidblocks.vault.VaultConstants.tenantServerPkiMountName

class TenantApplicationContext(
        val reference: TenantResource,
        val tenantRepository: TenantRepository,
        val isDevelopment: Boolean = false,
        val vaultAddressOverride: String? = null,
) {

    fun altNames() = if (isDevelopment) {
        listOf("localhost")
    } else {
        emptyList()
    }

    fun clientCertificateManager(client: String): VaultCertificateManager {
        val tenant = tenantRepository.getTenant(reference)

        return VaultCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(tenant.environment),
            token = tenant.environment.rootToken!!,
            pkiMount = tenantClientPkiMountName(reference),
            commonName = clientFQDN(client)
        )
    }

    fun serverCertificateManager(hostname: String): VaultCertificateManager {
        val tenant = tenantRepository.getTenant(reference)

        return VaultCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(tenant.environment),
            token = tenant.environment.rootToken!!,
            pkiMount = tenantServerPkiMountName(reference),
            commonName = tenantHostFQDN(hostname, reference, tenant.environment.cloud.rootDomain),
            altNames = altNames()
        )
    }

    fun serverCaCertificateManager(): VaultCaCertificateManager {
        val tenant = tenantRepository.getTenant(reference)

        return VaultCaCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(tenant.environment),
            token = tenant.environment.rootToken!!,
            pkiMount = tenantServerPkiMountName(reference),
        )
    }

    fun clientCaCertificateManager(): VaultCaCertificateManager {
        val tenant = tenantRepository.getTenant(reference)

        return VaultCaCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(tenant.environment),
            token = tenant.environment.rootToken!!,
            pkiMount = tenantClientPkiMountName(reference),
        )
    }
}
