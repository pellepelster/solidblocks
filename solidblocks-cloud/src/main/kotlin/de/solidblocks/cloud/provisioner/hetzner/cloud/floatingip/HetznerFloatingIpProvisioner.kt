package de.solidblocks.cloud.provisioner.hetzner.cloud.floatingip

import de.solidblocks.cloud.api.DestroyableResourceProvisioner
import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.resources.FloatingIpCreateRequest
import de.solidblocks.hetzner.cloud.resources.FloatingIpUpdateRequest
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerFloatingIpProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    InfrastructureResourceLookupProvider<HetznerFloatingIpLookup, HetznerFloatingIpRuntime>,
    InfrastructureResourceProvisioner<HetznerFloatingIp, HetznerFloatingIpRuntime, HetznerFloatingIpLookup>,
    DestroyableResourceProvisioner<HetznerFloatingIpLookup> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: HetznerFloatingIpLookup, context: SSHProvisionerContext) = api.floatingIps.get(lookup.name)?.let {
        HetznerFloatingIpRuntime(
            it.id,
            it.name,
            it.ip,
            it.type,
            it.server,
            it.labels,
            it.protection.delete,
        )
    }

    override suspend fun apply(resource: HetznerFloatingIp, context: ProvisionerApplyContext, log: LogContext): Result<HetznerFloatingIpRuntime> {
        val runtime = lookup(resource.asLookup(), context)

        logger.info { "creating ${resource.logText()} (${resource.type}) in '${resource.homeLocation}'" }
        val floatingIp =
            if (runtime == null) {
                api.floatingIps.create(
                    FloatingIpCreateRequest(
                        name = resource.name,
                        type = resource.type,
                        homeLocation = resource.homeLocation,
                        labels = resource.labels,
                    ),
                )
                lookup(resource.asLookup(), context)
            } else {
                runtime
            }

        if (floatingIp == null) {
            return Error("error creating ${resource.logText()}")
        }

        val protect = api.floatingIps.changeDeleteProtection(floatingIp.id, resource.protected)
        api.floatingIps.waitForAction(protect)

        api.floatingIps.update(floatingIp.id, FloatingIpUpdateRequest(resource.name, resource.labels))

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<HetznerFloatingIpRuntime>("error creating ${resource.logText()}")
    }

    override suspend fun diff(resource: HetznerFloatingIp, context: ProvisionerDiffContext): ResourceDiff? {
        val runtime = lookup(resource.asLookup(), context) ?: return ResourceDiff(resource, missing)

        val changes = mutableListOf<ResourceDiffItem>()

        if (runtime.type != resource.type) {
            changes.add(
                ResourceDiffItem(
                    "type",
                    changed = true,
                    triggersRecreate = true,
                    expectedValue = resource.type.toString(),
                    actualValue = runtime.type.toString(),
                ),
            )
        }

        if (runtime.deleteProtected != resource.protected) {
            changes.add(
                ResourceDiffItem(
                    "delete protection",
                    changed = true,
                    expectedValue = resource.protected.toString(),
                    actualValue = runtime.deleteProtected.toString(),
                ),
            )
        }

        changes.addAll(createLabelDiff(resource, runtime))

        return if (changes.isEmpty()) {
            ResourceDiff(resource, up_to_date)
        } else {
            ResourceDiff(resource, has_changes, changes = changes)
        }
    }

    override suspend fun destroy(lookup: HetznerFloatingIpLookup, context: SSHProvisionerContext, log: LogContext) = lookup(lookup, context)?.let {
        if (it.deleteProtected) {
            val unprotect = api.floatingIps.changeDeleteProtection(it.id, false)
            api.floatingIps.waitForAction(unprotect)
        }
        if (it.assigneeId != null) {
            api.floatingIps.unassign(it.id)?.let { action -> api.floatingIps.waitForAction(action) }
        }
        api.floatingIps.delete(it.id)
    } ?: false

    override val supportedLookupType: KClass<*> = HetznerFloatingIpLookup::class

    override val supportedResourceType: KClass<*> = HetznerFloatingIp::class
}
