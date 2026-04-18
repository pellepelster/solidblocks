package de.solidblocks.cloud.api.endpoint

data class Endpoint(val serverName: String, val address: String, val port: Int, val protocol: EndpointProtocol)
