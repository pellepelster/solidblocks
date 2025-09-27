package de.solidblocks.hetzner.dns.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginationResponse(
    val page: Int,
    @SerialName("per_page") val perPage: Int,
    @SerialName("last_page") val lastPage: Int,
    @SerialName("total_entries") val totalEntries: Int,
)
