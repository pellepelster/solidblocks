package de.solidblocks.ingress.config

data class Route(val match: List<Match>, val handle: List<Handler>)
