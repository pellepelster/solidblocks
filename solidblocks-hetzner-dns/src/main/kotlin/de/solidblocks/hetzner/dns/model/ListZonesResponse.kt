package de.solidblocks.hetzner.dns.model

import kotlinx.serialization.Serializable

@Serializable data class ListZonesResponse(val zones: List<ZoneResponse>, val meta: MetaResponse)
