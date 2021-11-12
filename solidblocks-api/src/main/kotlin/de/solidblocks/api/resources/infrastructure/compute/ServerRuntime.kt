package de.solidblocks.api.resources.infrastructure.compute

data class ServerRuntime(val id: String, val status: String, val labels: Map<String, String>, val hasVolumes: Boolean, val privateIp: String?, val publicIp: String?)
