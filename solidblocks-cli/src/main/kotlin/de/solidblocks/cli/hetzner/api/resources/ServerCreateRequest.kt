package de.solidblocks.cli.hetzner.api.resources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerCreateRequest(
    val name: String,
    @SerialName("server_type") val type: String,
    val image: String
)