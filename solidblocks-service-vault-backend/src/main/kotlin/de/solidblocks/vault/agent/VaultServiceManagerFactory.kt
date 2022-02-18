package de.solidblocks.vault.agent

import de.solidblocks.api.services.ServiceCatalogItemResponse
import de.solidblocks.api.services.ServiceManagerFactory

class VaultServiceManagerFactory : ServiceManagerFactory<VaultServiceManager> {

    override val type: String
        get() = "vault"

    override val catalogItem: ServiceCatalogItemResponse
        get() = ServiceCatalogItemResponse(type, "Secure, store and tightly control access to tokens, passwords, certificates, encryption keys for protecting secrets and other sensitive data using a UI, CLI, or HTTP API.")

    override fun createServiceManager(): VaultServiceManager {
        return VaultServiceManager()
    }
}
