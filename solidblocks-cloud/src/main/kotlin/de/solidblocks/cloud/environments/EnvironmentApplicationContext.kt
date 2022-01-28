package de.solidblocks.cloud.environments

import de.solidblocks.base.BaseConstants.environmentHostFQDN
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.model.ModelConstants.vaultAddress
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants.clientFQDN
import de.solidblocks.vault.VaultConstants.environmentClientPkiMountName
import de.solidblocks.vault.VaultConstants.environmentServerPkiMountName

class EnvironmentApplicationContext(
    val reference: EnvironmentReference,
    val environmentsRepository: EnvironmentsRepository,
    val isDevelopment: Boolean = false,
    val vaultAddressOverride: String? = null,
) {

    fun altNames() = if (isDevelopment) {
        listOf("localhost")
    } else {
        emptyList()
    }

    fun serverCertificateManager(hostname: String): VaultCertificateManager {
        val environment = environmentsRepository.getEnvironment(reference) ?: throw RuntimeException("environment '$reference' not found")

        return VaultCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentServerPkiMountName(reference),
            commonName = environmentHostFQDN(hostname, reference, rootDomain = environment.cloud.rootDomain),
            altNames = altNames()

        )
    }

    fun clientCertificateManager(client: String): VaultCertificateManager {
        val environment = environmentsRepository.getEnvironment(reference) ?: throw RuntimeException("environment '$reference' not found")

        return VaultCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentClientPkiMountName(reference),
            commonName = clientFQDN(client)

        )
    }

    fun serverCaCertificateManager(): VaultCaCertificateManager {
        val environment = environmentsRepository.getEnvironment(reference) ?: throw RuntimeException("environment '$reference' not found")

        return VaultCaCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentServerPkiMountName(reference),
        )
    }

    fun clientCaCertificateManager(): VaultCaCertificateManager {
        val environment = environmentsRepository.getEnvironment(reference) ?: throw RuntimeException("environment '$reference' not found")

        return VaultCaCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentClientPkiMountName(reference),
        )
    }
}
