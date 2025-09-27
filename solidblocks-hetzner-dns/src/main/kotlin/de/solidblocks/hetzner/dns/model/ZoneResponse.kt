package de.solidblocks.hetzner.dns.model

import de.solidblocks.hetzner.dns.MultiFormatInstantDeserializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@Serializable
@OptIn(ExperimentalTime::class)
data class ZoneResponse(
    val id: String,
    val name: String,
    val owner: String,
    val paused: Boolean? = null,
    val project: String,
    val status: String,
    val ttl: Int? = null ,
    @Serializable(with = MultiFormatInstantDeserializer::class)
    val created: Instant,
    @SerialName("legacy_ns") val legacyNs: List<String> = emptyList(),
    val ns: List<String> = emptyList(),
    @SerialName("records_count") val recordsCount: Int ? = null,
    val modified: String? = null,
    val verified: String? = null,
    val permission: String? = null,
    val registrar: String? = null,
    @SerialName("legacy_dns_host") val legacyDnsHost: String? = null,
    @SerialName("is_secondary_dns") val isSecondaryDns: Boolean = false,
    @SerialName("txt_verification") val txtVerification: TxtVerification? = null,
)
