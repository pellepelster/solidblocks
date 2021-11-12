package de.solidblocks.provisioner.vault.pki

import de.solidblocks.provisioner.vault.pki.dto.PkiBackendRole

data class VaultPkiBackendRoleRuntime(
    val backendRole: PkiBackendRole,
    val keysExist: Boolean
)
