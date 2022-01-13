package de.solidblocks.provisioner.vault.pki

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

class VaultPkiBackendRole(
    override val name: String,
    val allowedDomains: List<String>,
    val allowSubdomains: Boolean,
    val allowLocalhost: Boolean = false,
    val ttl: String,
    val maxTtl: String,
    val serverFlag: Boolean,
    val clientFlag: Boolean,
    val generateLease: Boolean,
    val keyType: String,
    val keyBits: Int,
    override val mount: IVaultMountLookup
) :
    IVaultPkiBackendRoleLookup,
    IInfrastructureResource<VaultPkiBackendRole, VaultPkiBackendRoleRuntime> {

    override val parents = setOf(mount)
}
