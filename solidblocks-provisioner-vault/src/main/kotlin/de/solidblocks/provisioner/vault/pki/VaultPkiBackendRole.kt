package de.solidblocks.provisioner.vault.pki

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

/*
            .allowAnyName(true)
            .ttl(DEFAULT_CERTIFICATE_LIFETIME.toString())
            .maxTtl(MAX_CERTIFICATE_LIFETIME.toString())
            .generateLease(true)
            .keyType(KEY_TYPE)
            .keyBits(KEY_BITS)
 */
class VaultPkiBackendRole(
        val id: String,
        val allowAnyName: Boolean,
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

    override fun getParents() =  listOf(mount)
}
