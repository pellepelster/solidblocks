package de.solidblocks.provisioner.vault.ssh

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

class VaultSshBackendRole(
        val name: String,
        val keyType: String,
        val maxTtl: String,
        val ttl: String,
        val allowHostCertificates: Boolean,
        val allowUserCertificates: Boolean,
        val allowedUsers: String? = null,
        val defaultExtensions: VaultSshBackendRoleDefaultExtensions? = null,
        val mount: IVaultMountLookup
) :
        IVaultSshBackendRoleLookup,
        IInfrastructureResource<VaultSshBackendRole, VaultSshBackendRoleRuntime> {

    override fun mount(): IVaultMountLookup {
        return mount
    }

    override fun name(): String {
        return this.name
    }

    override fun getParents() = listOf(mount)

}
