package de.solidblocks.provisioner.vault.ssh

import de.solidblocks.provisioner.vault.ssh.dto.SshBackendRole

data class VaultSshBackendRoleRuntime(
    val backendRole: SshBackendRole,
    val keysExist: Boolean
)
