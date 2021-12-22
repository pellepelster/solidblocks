package de.solidblocks.provisioner.vault.mount

class VaultMountLookup(val id: String) : IVaultMountLookup {
    override fun id(): String = id
}
