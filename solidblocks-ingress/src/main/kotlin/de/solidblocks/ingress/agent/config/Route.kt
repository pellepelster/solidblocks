package de.solidblocks.ingress.agent.config

data class Route(val match: List<Match>, val handle: List<Handler>)
