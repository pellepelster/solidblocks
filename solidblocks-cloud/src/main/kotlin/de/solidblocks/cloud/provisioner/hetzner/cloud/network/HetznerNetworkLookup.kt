package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerNetworkLookup(name: String) : InfrastructureResourceLookup<HetznerNetworkRuntime>(name, emptySet())
