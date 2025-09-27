package de.solidblocks.hetzner.dns.model

import kotlinx.serialization.Serializable

@Serializable
data class TxtVerification(
    val name: String? = null,
    val token: String? = null,
)
