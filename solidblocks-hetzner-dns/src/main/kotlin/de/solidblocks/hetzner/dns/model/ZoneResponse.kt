package de.solidblocks.hetzner.dns.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import de.solidblocks.hetzner.dns.MultiFormatInstantDeserializer
import java.time.Instant

data class TxtVerification(
    val name: String? = null,
    val token: String? = null
)

data class ZoneResponse(

    val id: String,
    val name: String,
    val owner: String,
    val paused: Boolean,
    val project: String,
    val status: String,
    val ttl: Int,

    @JsonDeserialize(using = MultiFormatInstantDeserializer::class)
    val created: Instant,

    @JsonProperty("legacy_ns")
    val legacyNs: List<String> = emptyList(),

    val ns: List<String> = emptyList(),

    @JsonProperty("records_count")
    val recordsCount: Int,

    val modified: String? = null,
    val verified: String? = null,
    val permission: String? = null,
    val registrar: String? = null,

    @JsonProperty("legacy_dns_host")
    val legacyDnsHost: String? = null,

    @JsonProperty("is_secondary_dns")
    val isSecondaryDns: Boolean = false,

    @JsonProperty("txt_verification")
    val txtVerification: TxtVerification? = null
)
