package de.solidblocks.provisioner.vault.ssh.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SshBackendRoleDefaultExtensions(
    @JsonProperty("permit-pty")
    var permitPty: String? = null,
    @JsonProperty("permit-port-forwarding")
    var permitPortForwarding: String? = null
)
