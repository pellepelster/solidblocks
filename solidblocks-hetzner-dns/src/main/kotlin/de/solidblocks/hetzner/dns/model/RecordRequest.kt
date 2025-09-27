package de.solidblocks.hetzner.dns.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordRequest(
    @SerialName("zone_id") val zoneId: String? = null,
    val type: RecordType? = null,
    val name: String? = null,
    val value: String? = null,
    val ttl: Int? = null,
)
