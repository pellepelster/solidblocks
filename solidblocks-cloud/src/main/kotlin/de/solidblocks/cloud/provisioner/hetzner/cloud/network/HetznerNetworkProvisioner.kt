package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.hetzner.cloud.resources.NetworkCreateRequest
import de.solidblocks.hetzner.cloud.resources.NetworkUpdateRequest
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerNetworkProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerNetworkLookup, HetznerNetworkRuntime>,
    InfrastructureResourceProvisioner<HetznerNetwork, HetznerNetworkRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: HetznerNetworkLookup, context: ProvisionerContext) =
        api.networks.get(lookup.name)?.let { network ->
            HetznerNetworkRuntime(network.id, network.name, network.protection.delete, network.labels, network.subnets.map {
                HetznerSubnetRuntime(it.ipRange, network.id)
            })
        }

    override suspend fun apply(resource: HetznerNetwork, context: ProvisionerContext, log: LogContext): ApplyResult<HetznerNetworkRuntime> {
        val runtime = lookup(resource.asLookup(), context)

        val network =
            if (runtime == null) {
                api.networks.create(
                    NetworkCreateRequest(
                        resource.name,
                        resource.ipRange,
                        resource.labels
                    ),
                )
                lookup(resource.asLookup(), context)
            } else {
                runtime
            }

        if (network == null) {
            return ApplyResult(null)
        }

        val protect = api.networks.changeDeleteProtection(network.id, resource.protected)
        api.networks.waitForAction(protect)

        api.networks.update(network.id, NetworkUpdateRequest(resource.name, resource.labels))

        return ApplyResult(lookup(resource.asLookup(), context))
    }

    override suspend fun diff(resource: HetznerNetwork, context: ProvisionerContext): ResourceDiff? {
        val runtime = lookup(resource.asLookup(), context) ?: return ResourceDiff(resource, missing)

        val deleteProtection =
            if (runtime.deleteProtected != resource.protected) {
                listOf(
                    ResourceDiffItem(
                        "delete protection",
                        changed = true,
                        expectedValue = resource.protected.toString(),
                        actualValue = runtime.deleteProtected.toString(),
                    ),
                )
            } else {
                emptyList()
            }

        val changes = createLabelDiff(resource, runtime) + deleteProtection

        return if (changes.isEmpty()) {
            ResourceDiff(
                resource,
                up_to_date,
            )
        } else {
            ResourceDiff(
                resource,
                has_changes,
                changes = changes,
            )
        }
    }

    override suspend fun destroy(
        resource: HetznerNetwork,
        context: ProvisionerContext,
        logContext: LogContext,
    ) = lookup(resource.asLookup(), context)?.let { api.networks.delete(it.id) } ?: false

    override val supportedLookupType: KClass<*> = HetznerNetworkLookup::class

    override val supportedResourceType: KClass<*> = HetznerNetwork::class
}
