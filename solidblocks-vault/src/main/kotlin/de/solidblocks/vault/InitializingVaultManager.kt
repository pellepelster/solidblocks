package de.solidblocks.vault

import mu.KotlinLogging
import org.springframework.vault.support.VaultInitializationRequest

class InitializingVaultManager(address: String) : BaseVaultManager(address) {

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
}
