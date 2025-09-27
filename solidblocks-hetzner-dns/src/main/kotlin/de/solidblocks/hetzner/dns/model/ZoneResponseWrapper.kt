package de.solidblocks.hetzner.dns.model

import kotlinx.serialization.Serializable

@Serializable
data class ZoneResponseWrapper(val zone: ZoneResponse)
