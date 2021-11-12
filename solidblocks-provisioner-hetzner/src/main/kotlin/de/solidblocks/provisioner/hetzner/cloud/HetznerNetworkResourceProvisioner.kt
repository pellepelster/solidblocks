package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.network.NetworkRuntime
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.general.Subnet
import me.tomsdevsn.hetznercloud.objects.general.SubnetType
import me.tomsdevsn.hetznercloud.objects.request.NetworkRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerNetworkResourceProvisioner(credentialsProvider: HetznerCloudCredentialsProvider) :
    BaseHetznerProvisioner<Network, NetworkRuntime, HetznerCloudAPI>(
        { HetznerCloudAPI(credentialsProvider.defaultApiToken()) },
        Network::class.java
    ) {

    private val logger = KotlinLogging.logger {}

    override fun apply(
        resource: Network
    ): Result<*> {

        val request = NetworkRequest.builder()
        request.name(resource.name)
        request.ipRange("10.0.0.0/16")

        val subnet = Subnet()
        subnet.ipRange = "10.0.1.0/24"
        subnet.type = SubnetType.server
        subnet.networkZone = "eu-central"
        request.subnets(listOf(subnet))

        return checkedApiCall(resource, HetznerCloudAPI::createNetwork) {
            it.createNetwork(request.build())
        }
    }

    override fun diff(resource: Network): Result<ResourceDiff<NetworkRuntime>> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, false)
            },
            {
                ResourceDiff(resource, true)
            }
        )
    }

    override fun lookup(resource: Network): Result<NetworkRuntime> {
        return checkedApiCall(resource, HetznerCloudAPI::getNetworksByName) {
            it.getNetworksByName(resource.name).networks.firstOrNull()
        }.mapNonNullResult {
            NetworkRuntime(it.id.toString())
        }
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
}
