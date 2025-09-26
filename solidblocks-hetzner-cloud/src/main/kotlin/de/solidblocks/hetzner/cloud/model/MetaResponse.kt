package de.solidblocks.hetzner.cloud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ListResponse<T> {
  val meta: MetaResponse
  val list: List<T>
}

@Serializable data class MetaResponse(val pagination: Pagination)

@Serializable
data class Pagination(
    @SerialName("last_page") val last_page: Int,
    @SerialName("next_page") val next_page: Int?,
    @SerialName("page") val page: Int,
    @SerialName("per_page") val per_page: Int,
    @SerialName("previous_page") val previous_page: Int?,
    @SerialName("total_entries") val total_entries: Int,
)
