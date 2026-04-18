package de.solidblocks.cloud.configuration.model

data class EnvironmentContext(val cloud: String, val environment: String)

data class CloudReference(val cloud: String)
