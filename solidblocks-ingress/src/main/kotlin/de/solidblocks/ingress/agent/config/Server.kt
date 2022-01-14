package de.solidblocks.ingress.agent.config

import com.fasterxml.jackson.annotation.JsonProperty

data class Server(
    val listen: List<String> = listOf(":80", ":443"),
    val routes: List<Route> = emptyList(),
    @JsonProperty("automatic_https") val automaticHttps: AutomaticHttps
)
