package de.solidblocks.provisioner.vault.pki

import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRoleDefaultExtensions
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRoleRuntime

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
    val mount: VaultMount
) :
    IInfrastructureResource<VaultPkiBackendRoleRuntime> {

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
