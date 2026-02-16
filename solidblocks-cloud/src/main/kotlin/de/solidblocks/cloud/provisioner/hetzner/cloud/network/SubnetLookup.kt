package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.ResourceLookup

data class SubnetLookup(
    override val name: String,
    val network: de.solidblocks.cloud.provisioner.hetzner.cloud.network.NetworkLookup,
) : ResourceLookup<SubnetRuntime>
