package de.solidblocks.provisioner.vault.kv

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.VaultMount

class VaultKV(val path: String, val data: Map<String, Any>, val mount: VaultMount) :
    IInfrastructureResource<VaultKVRuntime> {

    override fun getParents(): List<IInfrastructureResource<*>> {
        return listOf(mount)
    }

    override fun name(): String {
        return this.path
    }
}
