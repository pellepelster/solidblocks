package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.hetzner.cloud.resources.NetworkType
import de.solidblocks.hetzner.cloud.resources.NetworkZone
import de.solidblocks.hetzner.cloud.resources.NetworksSubnetCreateRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerSubnetProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerSubnetLookup, HetznerSubnetRuntime>,
    ResourceProvisioner<HetznerSubnet, HetznerSubnetRuntime, HetznerSubnetLookup> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: HetznerSubnetLookup, context: SSHProvisionerContext): HetznerSubnetRuntime? {
        val network = context.lookup(lookup.network)

        if (network == null) {
            logger.info { "${lookup.network.logText()} not found" }
            return null
        }

        logger.debug { "${network.logText()} has subnets '${network.subnets.joinToString(",")}'" }

        return network.subnets.singleOrNull { it.subnet == lookup.name }
    }

    override suspend fun apply(resource: HetznerSubnet, context: ProvisionerApplyContext): Result<HetznerSubnetRuntime> {
        val network =
            context.lookup(resource.network) ?: return Error("${resource.network.logText()} not found")

        if (network.subnets.none { it.subnet == resource.subnet }) {
            val response =
                api.networks.addSubnet(
                    network.id,
                    NetworksSubnetCreateRequest(
                        NetworkType.cloud,
                        resource.subnet,
                        NetworkZone.`eu-central`,
                    ),
                )
            api.networks.waitForAction(response)
        }

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<HetznerSubnetRuntime>("error creating ${resource.logText()}")
    }

    override suspend fun diff(resource: HetznerSubnet, context: ProvisionerDiffContext): Result<ResourceDiff> {
        val network = context.lookup(resource.network) ?: return Success(ResourceDiff(resource, missing))

        return Success(
            if (network.subnets.none { it.subnet == resource.subnet }) {
                ResourceDiff(resource, missing)
            } else {
                ResourceDiff(resource, up_to_date)
            },
        )
    }

    override val supportedLookupType: KClass<*> = HetznerSubnetLookup::class

    override val supportedResourceType: KClass<*> = HetznerSubnet::class
}
