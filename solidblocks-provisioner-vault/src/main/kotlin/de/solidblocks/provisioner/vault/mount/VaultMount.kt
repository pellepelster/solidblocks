package de.solidblocks.provisioner.vault.mount

import de.solidblocks.core.IInfrastructureResource

data class VaultMount(override val name: String, val type: String) :
    IVaultMountLookup,
    IInfrastructureResource<VaultMount, VaultMountRuntime>
