package de.solidblocks.hetzner.dns.model

import com.fasterxml.jackson.annotation.JsonProperty

public data class RecordRequest(
    @JsonProperty("zone_id") val zoneId: String? = null,
    val type: RecordType? = null,
    val name: String? = null,
    val value: String? = null,
    val ttl: Int? = null,
)
