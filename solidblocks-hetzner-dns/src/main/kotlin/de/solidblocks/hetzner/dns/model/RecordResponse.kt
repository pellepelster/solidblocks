package de.solidblocks.hetzner.dns.model

import de.solidblocks.hetzner.dns.MultiFormatInstantDeserializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@OptIn(ExperimentalTime::class)
data class RecordResponse(
    val id: String,
    @SerialName("zone_id") val zoneId: String,
    val name: String,
    val value: String,
    val ttl: Int? = null,
    val type: RecordType,
    @Serializable(with = MultiFormatInstantDeserializer::class) val created: Instant,
    @Serializable(with = MultiFormatInstantDeserializer::class) val modified: Instant? = null,
)
