package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerSubnetLookup(
    name: String,
    val network: HetznerNetworkLookup,
) : InfrastructureResourceLookup<HetznerSubnetRuntime>(name, setOf(network)) {}
