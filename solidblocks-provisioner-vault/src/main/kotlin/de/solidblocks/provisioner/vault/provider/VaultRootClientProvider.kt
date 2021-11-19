package de.solidblocks.provisioner.vault.provider

import de.solidblocks.api.resources.infrastructure.IInfrastructureClientProvider
import de.solidblocks.cloud.config.CloudConfigurationManager
import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultInitializationRequest
import java.net.URI

class VaultRootClientProvider(
        private val cloudName: String,
        private val environmentName: String,
        private val address: String,
        val configurationManager: CloudConfigurationManager
) :
    IInfrastructureClientProvider<VaultTemplate> {

    private val logger = KotlinLogging.logger {}

    override fun createClient(): VaultTemplate {

        val vaultTemplate = VaultTemplate(
            VaultEndpoint.from(URI.create(address))
        )

        val isInitialized = vaultTemplate.opsForSys().isInitialized

        if (!isInitialized) {
            logger.info { "initializing vault at '$address'" }
            val request = VaultInitializationRequest.create(5, 3)
            val response = vaultTemplate.opsForSys().initialize(request)

            configurationManager.updateEnvironment(cloudName, environmentName, response.keys.mapIndexed { i, key -> "vault-unseal-key-$i" to key }.toMap())
            configurationManager.updateEnvironment(cloudName, environmentName, "vault-root-token", response.rootToken.token)
        }

        val environment = configurationManager.environmentByName(cloudName, environmentName)

        val unsealStatus = vaultTemplate.opsForSys().unsealStatus
        if (unsealStatus.isSealed) {
            logger.info { "unsealing vault at '$address'" }

            environment.configValues.filter { it.name.startsWith("vault-unseal-key-") }.forEach {
                vaultTemplate.opsForSys().unseal(it.value)
            }
        }

        val rootToken = environment.configValues.firstOrNull { it.name == "vault-root-token" }
                ?: throw RuntimeException("vault at '$address is initialized, but no vault root token found for cloud '$cloudName'")

        return VaultTemplate(
            VaultEndpoint.from(URI.create(address)),
            TokenAuthentication(rootToken.value)
        )
    }

    override fun providerType(): Class<VaultTemplate> {
        return VaultTemplate::class.java
    }
}
