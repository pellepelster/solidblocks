package de.solidblocks.provisioner.vault.provider

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.vault.Vault.Constants.Companion.vaultAddress
import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultInitializationRequest
import java.net.URI

class VaultRootClientProvider(
    val cloudName: String,
    val environmentName: String,
    val configurationManager: CloudConfigurationManager,
    val vaultAddress: String? = null
) {
    private val logger = KotlinLogging.logger {}

    private var vaultTemplate: VaultTemplate? = null

    fun createClient(): VaultTemplate {

        val configurationContext = configurationManager.environmentByName(cloudName, environmentName)

        val vaultAddr = if (!vaultAddress.isNullOrEmpty()) vaultAddress else vaultAddress(configurationContext)

        if (vaultTemplate != null) {
            logger.info { "creating vault client for '$vaultAddr'" }
            return vaultTemplate!!
        }

        var vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(vaultAddr)))

        val isInitialized = vaultTemplate.opsForSys().isInitialized

        if (!isInitialized) {
            logger.info { "initializing vault at '$vaultAddr'" }
            val request = VaultInitializationRequest.create(5, 3)
            val response = vaultTemplate.opsForSys().initialize(request)

            configurationManager.updateEnvironment(cloudName, environmentName, response.keys.mapIndexed { i, key -> "vault-unseal-key-$i" to key }.toMap())
            configurationManager.updateEnvironment(cloudName, environmentName, "vault-root-token", response.rootToken.token)
        }

        val environment = configurationManager.environmentByName(cloudName, environmentName)

        val unsealStatus = vaultTemplate.opsForSys().unsealStatus
        if (unsealStatus.isSealed) {
            logger.info { "unsealing vault at '$vaultAddr'" }

            environment.configValues.filter { it.name.startsWith("vault-unseal-key-") }.forEach {
                vaultTemplate.opsForSys().unseal(it.value)
            }
        }

        val rootToken = environment.configValues.firstOrNull { it.name == "vault-root-token" }
            ?: throw RuntimeException("vault at '$vaultAddr is initialized, but no vault root token found for cloud '$cloudName'")

        vaultTemplate = VaultTemplate(
            VaultEndpoint.from(URI.create(vaultAddr)),
            TokenAuthentication(rootToken.value)
        )

        return vaultTemplate
    }
}
