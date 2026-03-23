package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime

class HetznerNetworkRuntime(val id: Long, val name: String, val ipRange: String, val deleteProtected: Boolean, labels: Map<String, String>, val subnets: List<HetznerSubnetRuntime>) : BaseLabeledInfrastructureResourceRuntime(labels) {
    override fun logText() = "network '$name' (${ipRange})"
}