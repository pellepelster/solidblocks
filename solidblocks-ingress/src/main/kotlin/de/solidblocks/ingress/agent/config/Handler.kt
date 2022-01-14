package de.solidblocks.ingress.agent.config

data class Handler(val handler: String = "reverse_proxy", val transport: Transport, val upstreams: List<Upstream>)
