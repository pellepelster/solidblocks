package de.solidblocks.provisioner.vault

import de.solidblocks.vault.InitializingVaultManager
import mu.KotlinLogging

class VaultHealthCheck {


    companion object {

        private val logger = KotlinLogging.logger {}

        fun check(vaultAddress: String): Boolean {
            val vaultManager = InitializingVaultManager(vaultAddress)

            if (!vaultManager.isInitialized()) {
                logger.error { "vault healtcheck failed '${vaultAddress}' is not initialized" }
                return false
            }

            if (vaultManager.isSealed()) {
                logger.error { "vault healtcheck failed '${vaultAddress}' is sealed" }
                return false
            }

            logger.info { "vault healtcheck succeeded for '${vaultAddress}'" }
            return true
        }
    }

}