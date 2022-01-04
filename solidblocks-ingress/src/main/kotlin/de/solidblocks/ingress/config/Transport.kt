package de.solidblocks.ingress.config

data class Transport(
    val protocol: String = "http",
    val compression: Boolean = true,
    val tls: Tls
)
