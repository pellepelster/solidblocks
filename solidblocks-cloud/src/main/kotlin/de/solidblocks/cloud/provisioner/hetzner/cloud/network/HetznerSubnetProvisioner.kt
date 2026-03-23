package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.resources.NetworkType
import de.solidblocks.hetzner.cloud.resources.NetworkZone
import de.solidblocks.hetzner.cloud.resources.NetworksSubnetCreateRequest
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerSubnetProvisioner(hcloudToken: String) : BaseHetznerProvisioner(hcloudToken), ResourceLookupProvider<HetznerSubnetLookup, HetznerSubnetRuntime>,
    InfrastructureResourceProvisioner<HetznerSubnet, HetznerSubnetRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: HetznerSubnetLookup, context: ProvisionerContext): HetznerSubnetRuntime? {
        val network = context.lookup(lookup.network)

        if (network == null) {
            logger.info { "${lookup.network.logText()} not found" }
            return null
        }

        logger.debug { "${network.logText()} has subnets '${network.subnets.joinToString(",")}'" }

        return network.subnets.singleOrNull { it.subnet == lookup.name }
    }

    override suspend fun apply(resource: HetznerSubnet, context: ProvisionerContext, log: LogContext): Result<HetznerSubnetRuntime> {
        val network = context.lookup(resource.network) ?: return Error("${resource.network.logText()} not found")

        if (network.subnets.none { it.subnet == resource.subnet }) {
            val response = api.networks.addSubnet(network.id, NetworksSubnetCreateRequest(NetworkType.cloud, resource.subnet, NetworkZone.`eu-central`))
            api.networks.waitForAction(response)
        }

        return lookup(resource.asLookup(), context)?.let {
            Success(it)
        } ?: Error<HetznerSubnetRuntime>("error creating ${resource.logText()}")
    }

    override suspend fun diff(resource: HetznerSubnet, context: ProvisionerContext): ResourceDiff? {
        val network = context.lookup(resource.network) ?: return ResourceDiff(resource, missing)

        return if (network.subnets.none { it.subnet == resource.subnet }) {
            ResourceDiff(resource, missing)
        } else {
            ResourceDiff(resource, up_to_date)
        }
    }

    override val supportedLookupType: KClass<*> = HetznerSubnetLookup::class

    override val supportedResourceType: KClass<*> = HetznerSubnet::class
}
