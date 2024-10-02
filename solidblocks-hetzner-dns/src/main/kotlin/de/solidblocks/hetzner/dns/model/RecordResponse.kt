package de.solidblocks.hetzner.dns.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import de.solidblocks.hetzner.dns.MultiFormatInstantDeserializer
import java.time.Instant

data class RecordResponse(
    val id: String,
    @JsonProperty("zone_id")
    val zoneId: String,
    val name: String,
    val value: String,
    val ttl: Int,
    val type: RecordType,
    @JsonDeserialize(using = MultiFormatInstantDeserializer::class)
    val created: Instant,

    @JsonDeserialize(using = MultiFormatInstantDeserializer::class)
    val modified: Instant?,
)
