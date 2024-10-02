package de.solidblocks.hetzner.dns.model

import com.fasterxml.jackson.annotation.JsonProperty

public data class Pagination(
    val page: Int,
    @JsonProperty("per_page") val perPage: Int,
    @JsonProperty("last_page") val lastPage: Int,
    @JsonProperty("total_entries") val totalEntries: Int
)

public data class MetaResponse(val pagination: Pagination)
