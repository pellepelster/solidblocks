package de.solidblocks.ingress.config

data class Handler(val handler: String = "reverse_proxy", val transport: Transport, val upstreams: List<Upstream>)
