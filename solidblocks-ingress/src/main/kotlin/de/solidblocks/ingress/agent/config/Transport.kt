package de.solidblocks.ingress.agent.config

data class Transport(
    val protocol: String = "http",
    val compression: Boolean = true,
    val tls: Tls
)
