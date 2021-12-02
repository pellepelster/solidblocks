package de.solidblocks.provisioner.vault.mount

import de.solidblocks.core.IInfrastructureResource

data class VaultMount(val id: String, val type: String) :
    IVaultMountLookup,
    IInfrastructureResource<VaultMount, VaultMountRuntime> {

    override fun id() = id
}
