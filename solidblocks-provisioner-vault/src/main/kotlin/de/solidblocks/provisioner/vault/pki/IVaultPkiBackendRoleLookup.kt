package de.solidblocks.provisioner.vault.pki

import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

interface IVaultPkiBackendRoleLookup : IResourceLookup<VaultPkiBackendRoleRuntime> {
    val mount: IVaultMountLookup
}
