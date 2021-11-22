package de.solidblocks.provisioner.vault.pki

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource
import de.solidblocks.core.IResourceLookup
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
        val name: String,
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

    override fun name(): String {
        return this.name
    }

    override fun getParents() =  listOf(mount)
}
