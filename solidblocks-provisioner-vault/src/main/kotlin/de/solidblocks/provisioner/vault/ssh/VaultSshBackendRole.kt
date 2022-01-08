package de.solidblocks.provisioner.vault.ssh

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

class VaultSshBackendRole(
    override val name: String,
    val keyType: String,
    val maxTtl: String,
    val ttl: String,
    val allowHostCertificates: Boolean,
    val allowUserCertificates: Boolean,
    val allowedUsers: List<String> = emptyList(),
    val defaultUser: String? = null,
    val defaultExtensions: VaultSshBackendRoleDefaultExtensions? = null,
    override val mount: IVaultMountLookup
) :
    IVaultSshBackendRoleLookup,
    IInfrastructureResource<VaultSshBackendRole, VaultSshBackendRoleRuntime> {

    override val parents = setOf(mount)
}
