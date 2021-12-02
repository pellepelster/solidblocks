package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IResourceLookup

interface ISubnetLookup : IResourceLookup<SubnetRuntime> {
    fun network(): INetworkLookup
}
