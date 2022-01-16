package de.solidblocks.provisioner.vault

import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.vault.InitializingVaultManager
import de.solidblocks.vault.VaultConstants.UNSEAL_KEY_PREFIX
import de.solidblocks.vault.model.VaultCredentials
import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.net.URI

class VaultRootClientProvider(
        private val reference: EnvironmentResource,
        private val environmentRepository: EnvironmentRepository,
        private val vaultAddressOverride: String? = null
) {

    private fun vaultAddress(): String {

        if (vaultAddressOverride != null) {
            return vaultAddressOverride
        }

        val environment = environmentRepository.getEnvironment(reference)

        return ModelConstants.vaultAddress(environment)
    }

    private val logger = KotlinLogging.logger {}

    private var vaultTemplate: VaultTemplate? = null

    private fun getVaultCredentials(): VaultCredentials {
        val environment = environmentRepository.getEnvironment(reference)

        val rootToken = environment.rootToken
            ?: throw RuntimeException("vault at '${vaultAddress()}' is initialized, but no vault root token found for cloud '${reference.cloud}'")
        val unsealKeys = environment.configValues.filter { it.name.startsWith(UNSEAL_KEY_PREFIX) }.map { it.value }

        return VaultCredentials(rootToken, unsealKeys)
    }

    fun createClient(): VaultTemplate {

        if (vaultTemplate != null) {
            return vaultTemplate!!
        }

        val environment = environmentRepository.getEnvironment(reference)

        val initializingVaultManager = InitializingVaultManager(vaultAddress())
        val hasUnsealKeys = environment.configValues.any { it.name.startsWith(UNSEAL_KEY_PREFIX) }

        if (!initializingVaultManager.isInitialized()) {

            if (hasUnsealKeys) {
                throw RuntimeException("environment '${reference.environment}' for cloud '${reference.cloud}' has unseal keys, but is not initialized")
            }

            val result = initializingVaultManager.initializeAndUnseal()
            environmentRepository.updateEnvironment(
                reference,
                result.unsealKeys.mapIndexed { i, key -> "$UNSEAL_KEY_PREFIX-$i" to key }.toMap()
            )
            environmentRepository.updateRootToken(reference, result.rootToken)
        }

        val credentials = getVaultCredentials()

        if (initializingVaultManager.isSealed()) {
            initializingVaultManager.unseal(credentials)
        }

        vaultTemplate = VaultTemplate(
            VaultEndpoint.from(URI.create(vaultAddress())),
            TokenAuthentication(credentials.rootToken)
        )

        return vaultTemplate!!
    }
}
