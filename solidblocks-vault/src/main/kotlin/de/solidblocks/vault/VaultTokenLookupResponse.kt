package de.solidblocks.vault

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Duration
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class VaultTokenLookupResponse(
    @JsonProperty("expire_time") val expireTime: Date,
    val orphan: Boolean,
    val renewable: Boolean,
    @JsonProperty("ttl") private val ttlRaw: Int
) {
    val ttl: Duration
        get() = Duration.ofSeconds(ttlRaw.toLong())
}
