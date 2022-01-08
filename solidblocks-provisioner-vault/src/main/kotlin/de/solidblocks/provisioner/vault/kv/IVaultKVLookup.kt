package de.solidblocks.provisioner.vault.kv

import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup

interface IVaultKVLookup : IResourceLookup<VaultKVRuntime> {
    val mount: IVaultMountLookup

    val path: String
}
