package de.solidblocks.provisioner.vault.ssh

import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.VaultMount

class VaultSshBackendRole(
    val name: String,
    val keyType: String,
    val maxTtl: String,
    val ttl: String,
    val allowHostCertificates: Boolean,
    val allowUserCertificates: Boolean,
    val allowedUsers: String? = null,
    val defaultExtensions: VaultSshBackendRoleDefaultExtensions? = null,
    val mount: VaultMount
) :
    IInfrastructureResource<VaultSshBackendRoleRuntime> {

    override fun getParents(): List<IInfrastructureResource<*>> {
        return listOf(mount)
    }

    override fun name(): String {
        return this.name
    }

    override fun getParentDataSources(): List<IDataSource<*>> {
        return emptyList()
    }
}
