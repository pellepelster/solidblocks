package de.solidblocks.vault

import de.solidblocks.vault.model.VaultCredentials
import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.net.URI

abstract class BaseVaultAdminManager(vaultTemplate: VaultTemplate) {

    private val logger = KotlinLogging.logger {}

    protected val vaultTemplate: VaultTemplate

    init {
        this.vaultTemplate = vaultTemplate
    }

    companion object {
        fun createVaultTemplate(address: String, token: String? = null): VaultTemplate {
            return if (token != null) {
                VaultTemplate(VaultEndpoint.from(URI.create(address)), TokenAuthentication(token))
            } else {
                VaultTemplate(VaultEndpoint.from(URI.create(address)))
            }
        }
    }

    constructor(address: String, token: String? = null) : this(createVaultTemplate(address, token))

    fun isInitialized(): Boolean {
        return vaultTemplate.opsForSys().isInitialized
    }

    fun isSealed(): Boolean {
        return vaultTemplate.opsForSys().unsealStatus.isSealed
    }

    fun unseal(vaultCredentials: VaultCredentials): Boolean {
        logger.info { "unsealing vault" }

        vaultCredentials.unsealKeys.forEach {
            vaultTemplate.opsForSys().unseal(it)
        }

        return true
    }
}
