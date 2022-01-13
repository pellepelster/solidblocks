package de.solidblocks.provisioner.vault.pki.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = false)
data class PkiBackendRole(
    val allowed_domains: List<String>,
    val allow_subdomains: Boolean,
    val allow_localhost: Boolean,
    val key_type: String?,
    val key_bits: Int?,
    val max_ttl: String?,
    val server_flag: Boolean?,
    val client_flag: Boolean?,
    val ttl: String?,
    val generate_lease: Boolean?,
)
