package de.solidblocks.provisioner.vault.pki

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

class VaultPkiBackendRole(
    val id: String,
    val allowedDomains: List<String>,
    val allowSubdomains: Boolean,
    val allowLocalhost: Boolean,
    val ttl: String,
    val maxTtl: String,
    val generateLease: Boolean,
    val keyType: String,
    val keyBits: Int,
    val mount: IVaultMountLookup
) :
    IVaultPkiBackendRoleLookup,
    IInfrastructureResource<VaultPkiBackendRole, VaultPkiBackendRoleRuntime> {

    override fun mount(): IVaultMountLookup {
        return mount
    }

    override fun id() = id

    override fun getParents() = setOf(mount)
}
