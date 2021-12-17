package de.solidblocks.vault

import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.net.URI

data class VaultCredentials(val rootToken: String, val unsealKeys: List<String>)

abstract class BaseVaultManager(val address: String, val token: String? = null) {

    private val logger = KotlinLogging.logger {}

    protected val vaultTemplate: VaultTemplate

    init {
        logger.info { "initializing vault manager for address '$address'" }
        if (token != null) {
            vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(address)), TokenAuthentication(token))
        } else {
            vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(address)))
        }
    }

    fun isInitialized(): Boolean {
        return vaultTemplate.opsForSys().isInitialized
    }

    fun isSealed(): Boolean {
        return vaultTemplate.opsForSys().unsealStatus.isSealed
    }

    fun unseal(vaultCredentials: VaultCredentials): Boolean {
        logger.info { "unsealing vault at '$address'" }

        vaultCredentials.unsealKeys.forEach {
            vaultTemplate.opsForSys().unseal(it)
        }

        return true
    }
}
