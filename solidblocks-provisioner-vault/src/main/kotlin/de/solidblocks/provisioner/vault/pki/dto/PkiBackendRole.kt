package de.solidblocks.provisioner.vault.pki.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = false)
data class PkiBackendRole(
    val key_type: String?,
    val key_bits: Int?,
    val max_ttl: String?,
    val ttl: String?,
    val allow_any_name: Boolean?,
    val generate_lease: Boolean?,
)
