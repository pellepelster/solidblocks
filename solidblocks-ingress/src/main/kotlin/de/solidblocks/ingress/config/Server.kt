package de.solidblocks.ingress.config

import com.fasterxml.jackson.annotation.JsonProperty

data class Server(
    val listen: List<String> = listOf(":80", ":443"),
    @JsonProperty("automatic_https") val automaticHttps: AutomaticHttps
)
