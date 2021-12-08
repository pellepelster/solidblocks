package de.solidblocks.provisioner.hetzner.cloud.network

import de.solidblocks.core.IResourceLookup

interface ISubnetLookup : IResourceLookup<SubnetRuntime> {
    fun network(): INetworkLookup
}
