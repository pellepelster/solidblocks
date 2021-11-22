package de.solidblocks.provisioner.vault.kv

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

class VaultKV(val path: String, val data: Map<String, Any>, val mount: IVaultMountLookup) :
        IVaultKVLookup,
        IInfrastructureResource<VaultKV, VaultKVRuntime> {

    override fun getParents() = listOf(mount)

    override fun mount(): IVaultMountLookup {
        return mount
    }

    override fun path(): String {
        return path
    }

    override fun name(): String {
        return "${mount.name()}.${this.path}"
    }
}
