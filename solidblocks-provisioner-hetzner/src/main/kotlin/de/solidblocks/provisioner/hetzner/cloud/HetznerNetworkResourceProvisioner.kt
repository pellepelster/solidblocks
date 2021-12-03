package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.network.INetworkLookup
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.network.NetworkRuntime
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.NetworkRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerNetworkResourceProvisioner(hetznerCloudAPI: HetznerCloudAPI) :
        IResourceLookupProvider<INetworkLookup, NetworkRuntime>,
        IInfrastructureResourceProvisioner<Network,
                NetworkRuntime>, BaseHetznerProvisioner<Network, NetworkRuntime, HetznerCloudAPI>(hetznerCloudAPI) {

    private val logger = KotlinLogging.logger {}

    override fun apply(
            resource: Network
    ): Result<*> {

        val request = NetworkRequest.builder()
        request.name(resource.id)
        request.ipRange(resource.ipRange)

        return checkedApiCall(resource, HetznerCloudAPI::createNetwork) {
            it.createNetwork(request.build())
        }
    }

    override fun diff(resource: Network): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, false)
            },
            {
                ResourceDiff(resource, true)
            }
        )
    }

    private fun destroy(id: Long): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::deleteNetwork) {
            it.deleteNetwork(id)
        }
    }

    override fun destroyAll(): Result<*> {
        logger.info { "destroying all networks" }

        return checkedApiCall(NullResource, HetznerCloudAPI::getAllNetworks) {
            it.allNetworks.networks
        }.mapNonNullResult {
            it.map { network ->
                logger.info { "destroying network '${network.name}'" }
                destroy(network.id)
            }.reduceResults()
        }
    }

    override fun destroy(resource: Network): Result<*> {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }
    }

    override fun getResourceType(): Class<*> {
        return Network::class.java
    }

    override fun lookup(lookup: INetworkLookup): Result<NetworkRuntime> {
        return checkedApiCall(lookup, HetznerCloudAPI::getNetworksByName) {
            it.getNetworksByName(lookup.id()).networks.firstOrNull()
        }.mapNonNullResult {
            NetworkRuntime(it.id.toString())
        }
    }

    override fun getLookupType(): Class<*> {
        return INetworkLookup::class.java
    }
}
