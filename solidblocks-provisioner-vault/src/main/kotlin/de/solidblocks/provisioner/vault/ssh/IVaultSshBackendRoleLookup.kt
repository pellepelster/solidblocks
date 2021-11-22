package de.solidblocks.provisioner.vault.ssh

import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

interface IVaultSshBackendRoleLookup : IResourceLookup<VaultSshBackendRoleRuntime> {
    fun mount(): IVaultMountLookup
}
