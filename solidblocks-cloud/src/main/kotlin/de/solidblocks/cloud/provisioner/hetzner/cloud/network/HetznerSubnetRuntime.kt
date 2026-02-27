package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

data class HetznerSubnetRuntime(val subnet: String, val network: HetznerNetworkLookup) :
    BaseInfrastructureResourceRuntime() {
}
