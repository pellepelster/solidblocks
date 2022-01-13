package de.solidblocks.cloud

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.VaultConstants.clientFQDN
import de.solidblocks.vault.VaultConstants.environmentClientPkiMountName
import de.solidblocks.vault.VaultConstants.environmentHostFQDN
import de.solidblocks.vault.VaultConstants.environmentServerPkiMountName
import de.solidblocks.vault.VaultConstants.vaultAddress

class EnvironmentApplicationContext(
    val reference: EnvironmentReference,
    val environmentRepository: EnvironmentRepository,
    val isDevelopment: Boolean = false,
    val vaultAddressOverride: String? = null,
) {

    fun altNames() = if (isDevelopment) {
        listOf("localhost")
    } else {
        emptyList()
    }

    fun serverCertificateManager(hostname: String): VaultCertificateManager {
        val environment = environmentRepository.getEnvironment(reference)

        return VaultCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentServerPkiMountName(reference),
            commonName = environmentHostFQDN(hostname, reference, rootDomain = environment.cloud.rootDomain),
            altNames = altNames()

        )
    }

    fun clientCertificateManager(client: String): VaultCertificateManager {
        val environment = environmentRepository.getEnvironment(reference)

        return VaultCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentClientPkiMountName(reference),
            commonName = clientFQDN(client)

        )
    }

    fun serverCaCertificateManager(): VaultCaCertificateManager {
        val environment = environmentRepository.getEnvironment(reference)

        return VaultCaCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentServerPkiMountName(reference),
        )
    }

    fun clientCaCertificateManager(): VaultCaCertificateManager {
        val environment = environmentRepository.getEnvironment(reference)

        return VaultCaCertificateManager(
            address = vaultAddressOverride ?: vaultAddress(environment),
            token = environment.rootToken!!,
            pkiMount = environmentClientPkiMountName(reference),
        )
    }
}
