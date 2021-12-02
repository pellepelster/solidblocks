package de.solidblocks.provisioner.vault.provider

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.Constants.Vault.Companion.vaultAddr
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultInitializationRequest
import java.net.URI

@Component
class VaultRootClientProvider(
    val configurationContext: CloudConfigurationContext,
    val configurationManager: CloudConfigurationManager,
    @Value("\${vault.addr:null}")
    val vaultAddrOverride: String? = null
) {

    private val logger = KotlinLogging.logger {}

    private var vaultTemplate: VaultTemplate? = null

    fun createClient(): VaultTemplate {
        val vaultAddr = listOfNotNull(vaultAddrOverride, vaultAddr(configurationContext.environment)).first()

        logger.info { "creating vault client for '$vaultAddr'" }

        if (vaultTemplate != null) {
            return vaultTemplate!!
        }

        var vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(vaultAddr)))

        val isInitialized = vaultTemplate.opsForSys().isInitialized

        if (!isInitialized) {
            logger.info { "initializing vault at '$vaultAddr'" }
            val request = VaultInitializationRequest.create(5, 3)
            val response = vaultTemplate.opsForSys().initialize(request)

            configurationManager.updateEnvironment(configurationContext.cloudName, configurationContext.environmentName, response.keys.mapIndexed { i, key -> "vault-unseal-key-$i" to key }.toMap())
            configurationManager.updateEnvironment(configurationContext.cloudName, configurationContext.environmentName, "vault-root-token", response.rootToken.token)
        }

        val environment = configurationManager.environmentByName(configurationContext.cloudName, configurationContext.environmentName)
            ?: throw RuntimeException("environment '${configurationContext.environmentName}' not found for cloud '${configurationContext.cloudName}'")

        val unsealStatus = vaultTemplate.opsForSys().unsealStatus
        if (unsealStatus.isSealed) {
            logger.info { "unsealing vault at '$vaultAddr'" }

            environment.configValues.filter { it.name.startsWith("vault-unseal-key-") }.forEach {
                vaultTemplate.opsForSys().unseal(it.value)
            }
        }

        val rootToken = environment.configValues.firstOrNull { it.name == "vault-root-token" }
            ?: throw RuntimeException("vault at '$vaultAddr is initialized, but no vault root token found for cloud '${configurationContext.cloudName}'")

        vaultTemplate = VaultTemplate(
            VaultEndpoint.from(URI.create(vaultAddr)),
            TokenAuthentication(rootToken.value)
        )

        return vaultTemplate
    }
}
