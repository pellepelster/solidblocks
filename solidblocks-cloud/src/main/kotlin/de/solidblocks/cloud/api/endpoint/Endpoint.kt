package de.solidblocks.cloud.api.endpoint

data class Endpoint(
    val address: String,
    val port: Int,
    val protocol: EndpointProtocol,
)
