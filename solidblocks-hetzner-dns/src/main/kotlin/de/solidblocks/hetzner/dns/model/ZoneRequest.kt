package de.solidblocks.hetzner.dns.model

public data class ZoneRequest(
    val name: String? = null,
    val ttl: Int? = null,
)
