package de.solidblocks.provisioner.hetzner.cloud.floatingip

data class FloatingIpRuntime(val id: String, val ipv4: String, val server: Long?)
