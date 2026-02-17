package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.hetzner.cloud.resources.VolumeCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeFormat
import de.solidblocks.hetzner.cloud.resources.VolumeUpdateRequest
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerVolumeProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<VolumeLookup, VolumeRuntime>,
    InfrastructureResourceProvisioner<Volume, VolumeRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun lookup(lookup: VolumeLookup, context: ProvisionerContext) =
      api.volumes.get(lookup.name)?.let {
        VolumeRuntime(it.id, it.name, it.linuxDevice, it.server, it.labels, it.protection.delete)
      }

  override suspend fun apply(
      resource: Volume,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<VolumeRuntime> {
    val runtime = lookup(resource.asLookup(), context)

    val volume =
        if (runtime == null) {
          api.volumes.create(
              VolumeCreateRequest(
                  resource.name,
                  resource.size,
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
      return ApplyResult(null)
    }

    val protect = api.volumes.changeDeleteProtection(volume.id, resource.protected)
    api.volumes.waitForAction(protect)

    api.volumes.update(volume.id, VolumeUpdateRequest(resource.name, resource.labels))

    return ApplyResult(lookup(resource.asLookup(), context))
  }

  override suspend fun diff(resource: Volume, context: ProvisionerContext): ResourceDiff? {
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
      resource: Volume,
      context: ProvisionerContext,
      logContext: LogContext,
  ) = lookup(resource.asLookup(), context)?.let { api.volumes.delete(it.id) } ?: false

  override val supportedLookupType: KClass<*> = VolumeLookup::class

  override val supportedResourceType: KClass<*> = Volume::class
}
