package de.solidblocks.provisioner.hetzner.cloud.network

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.core.Result
import de.solidblocks.core.Result.Companion.emptyResult
import de.solidblocks.core.Result.Companion.failedResult
import de.solidblocks.core.Result.Companion.resultOf
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.general.SubnetType
import me.tomsdevsn.hetznercloud.objects.request.AddSubnetToNetworkRequest
import mu.KotlinLogging

class HetznerSubnetResourceProvisioner(
    hetznerCloudAPI: HetznerCloudAPI,
    val provisioner: InfrastructureProvisioner
) :
    IResourceLookupProvider<ISubnetLookup, SubnetRuntime>,
    IInfrastructureResourceProvisioner<Subnet, SubnetRuntime>,
    BaseHetznerProvisioner<Subnet, SubnetRuntime, HetznerCloudAPI>(hetznerCloudAPI) {

    private val logger = KotlinLogging.logger {}

    override fun apply(resource: Subnet): Result<*> {
        val request = AddSubnetToNetworkRequest.builder()
            .networkZone("eu-central")
            .ipRange(resource.subnet)
            .type(SubnetType.server.toString())

        return this.provisioner.lookup(resource.network).mapNonNullResult { networkRuntime ->
            checkedApiCall {
                it.addSubnetToNetwork(networkRuntime.id.toLong(), request.build())
            }
        }
    }

    override fun diff(resource: Subnet): Result<ResourceDiff> {
        val result = lookup(resource)

        if (result.isEmpty()) {
            return resultOf(ResourceDiff(resource, true))
        }

        return resultOf(ResourceDiff(resource, false))
    }

    override fun lookup(lookup: ISubnetLookup): Result<SubnetRuntime> {
        val result = checkedApiCall {
            it.getNetworksByName(lookup.network().name).networks.firstOrNull()
        }

        if (result.isEmptyOrFailed()) {
            return failedResult()
        }

        val subnet = result.result!!.subnets.firstOrNull {
            it.ipRange == lookup.name
        }

        if (subnet != null) {
            return Result(result = SubnetRuntime(subnet.ipRange))
        }

        return emptyResult()
    }

    override val resourceType = Subnet::class.java

    override val lookupType = ISubnetLookup::class.java
}
