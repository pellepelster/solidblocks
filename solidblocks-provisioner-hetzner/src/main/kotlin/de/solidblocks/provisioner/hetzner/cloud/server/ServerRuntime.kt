package de.solidblocks.provisioner.hetzner.cloud.server

data class ServerRuntime(val id: String, val status: String, val labels: Map<String, String>, val hasVolumes: Boolean, val privateIp: String?, val publicIp: String?)
