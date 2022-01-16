package de.solidblocks.vault

import de.solidblocks.base.resources.ServiceResource
import org.springframework.vault.core.VaultTemplate

class ServiceVaultManager(vaultTemplate: VaultTemplate, val reference: ServiceResource) :
    BaseVaultManager(vaultTemplate) {

    constructor(address: String, token: String, reference: ServiceResource) : this(
        createVaultTemplate(
            address, token
        ),
        reference
    )

    override fun kvPath(path: String) = "/${VaultConstants.kvMountName(reference.asTenant())}/data/$path"
}
