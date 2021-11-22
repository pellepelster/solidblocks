package de.solidblocks.provisioner.vault.mount

import de.solidblocks.core.IInfrastructureResource

data class VaultMount(val name: String, val type: String) :
        IVaultMountLookup,
        IInfrastructureResource<VaultMount, VaultMountRuntime> {

    override fun name(): String {
        return this.name
    }
}
