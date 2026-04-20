package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.resources.VolumeCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeFormat
import de.solidblocks.hetzner.cloud.resources.VolumeUpdateRequest
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerVolumeProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerVolumeLookup, HetznerVolumeRuntime>,
    InfrastructureResourceProvisioner<HetznerVolume, HetznerVolumeRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: HetznerVolumeLookup, context: ProvisionerContext) = api.volumes.get(lookup.name)?.let {
        HetznerVolumeRuntime(
            it.id,
            it.name,
            it.linuxDevice,
            it.server,
            it.labels,
            it.protection.delete,
        )
    }

    override suspend fun apply(resource: HetznerVolume, context: ProvisionerApplyContext, log: LogContext): Result<HetznerVolumeRuntime> {
        val runtime = lookup(resource.asLookup(), context)

        logger.info { "creating volume '${resource.name}' with size ${resource.size.gigabytes()}" }
        val volume =
            if (runtime == null) {
                api.volumes.create(
                    VolumeCreateRequest(
                        resource.name,
                        resource.size.gigabytes(),
                        resource.location,
                        format = VolumeFormat.ext4,
                        automount = false,
                        resource.labels,
                    ),
                )
                lookup(resource.asLookup(), context)
            } else {
                runtime
            }

        if (volume == null) {
            return Error("error creating ${resource.logText()}")
        }

        val protect = api.volumes.changeDeleteProtection(volume.id, resource.protected)
        api.volumes.waitForAction(protect)

        api.volumes.update(volume.id, VolumeUpdateRequest(resource.name, resource.labels))

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<HetznerVolumeRuntime>("error creating ${resource.logText()}")
    }

    override suspend fun diff(resource: HetznerVolume, context: ProvisionerDiffContext): ResourceDiff? {
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

    override suspend fun destroy(resource: HetznerVolume, context: ProvisionerContext, log: LogContext) = lookup(resource.asLookup(), context)?.let { api.volumes.delete(it.id) } ?: false

    override val supportedLookupType: KClass<*> = HetznerVolumeLookup::class

    override val supportedResourceType: KClass<*> = HetznerVolume::class
}
