package de.solidblocks.provisioner.hetzner.cloud.network

import de.solidblocks.core.IResourceLookup

interface ISubnetLookup : IResourceLookup<SubnetRuntime> {
    val network: INetworkLookup
}
