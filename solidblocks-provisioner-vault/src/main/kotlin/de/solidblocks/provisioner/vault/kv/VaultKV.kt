package de.solidblocks.provisioner.vault.kv

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

class VaultKV(override val path: String, val data: Map<String, Any>, override val mount: IVaultMountLookup) :
    IVaultKVLookup,
    IInfrastructureResource<VaultKV, VaultKVRuntime> {

    override val parents = setOf(mount)

    override val name = "${mount.name}.${this.path}"
}
