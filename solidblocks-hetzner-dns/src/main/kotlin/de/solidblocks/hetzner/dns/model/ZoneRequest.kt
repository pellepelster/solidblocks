package de.solidblocks.hetzner.dns.model


data class ZoneRequest(
    val name: String? = null,
    val ttl: Int? = null
)