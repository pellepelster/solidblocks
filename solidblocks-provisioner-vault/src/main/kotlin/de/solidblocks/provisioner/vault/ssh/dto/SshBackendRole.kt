package de.solidblocks.provisioner.vault.ssh.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = false)
data class SshBackendRole(
    val key_type: String?,
    val max_ttl: String?,
    val ttl: String?,
    val allow_host_certificates: Boolean?,
    val allow_user_certificates: Boolean?,
    val allowed_users: String? = null,
    val default_extensions: SshBackendRoleDefaultExtensions? = null

)
