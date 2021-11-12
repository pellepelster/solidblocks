package de.solidblocks.provisioner.vault.ssh

data class VaultSshBackendRoleDefaultExtensions(
    val permitPty: String,
    val permitPortForwarding: String
)
