package de.solidblocks.vault

import de.solidblocks.vault.model.VaultCredentials
import mu.KotlinLogging
import org.springframework.vault.support.VaultInitializationRequest

class InitializingVaultManager(val address: String) : BaseVaultAdminManager(address) {

    private val logger = KotlinLogging.logger {}

    fun initialize(): VaultCredentials {
        logger.info { "initializing vault" }
        val request = VaultInitializationRequest.create(5, 3)
        val response = vaultTemplate.opsForSys().initialize(request)

        return VaultCredentials(response.rootToken.token, response.keys)
    }

    fun initializeAndUnseal(): VaultCredentials {
        val initializeResult = initialize()
        unseal(initializeResult)
        return initializeResult
    }

    fun seal(): Boolean {
        logger.info { "sealing vault at address '$address'" }
        vaultTemplate.opsForSys().seal()
        return true
    }
}
