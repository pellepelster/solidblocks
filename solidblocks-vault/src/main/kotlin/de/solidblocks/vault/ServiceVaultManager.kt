package de.solidblocks.vault

import de.solidblocks.base.ServiceReference
import org.springframework.vault.core.VaultTemplate

class ServiceVaultManager(vaultTemplate: VaultTemplate, reference: ServiceReference) :
    BaseVaultManager<ServiceReference>(vaultTemplate, reference) {

    constructor(address: String, token: String, reference: ServiceReference) : this(
        createVaultTemplate(
            address, token
        ),
        reference
    )

    override fun kvPath(path: String) = "/${VaultConstants.kvMountName(reference.asTenant())}/data/$path"
}
