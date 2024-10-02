package de.solidblocks.hetzner.dns.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Pagination(
    val page: Int,
    @JsonProperty("per_page") val perPage: Int,
    @JsonProperty("last_page") val lastPage: Int,
    @JsonProperty("total_entries") val totalEntries: Int
)

data class Meta(val pagination: Pagination)
