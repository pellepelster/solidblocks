package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class Resource2Provisioner :
    ResourceLookupProvider<Resource2Lookup, Resource2Runtime>,
    InfrastructureResourceProvisioner<Resource2, Resource2Runtime> {

  private val logger = KotlinLogging.logger {}

  val resources = mutableMapOf<String, Resource2>()

  override suspend fun lookup(lookup: Resource2Lookup, context: ProvisionerContext) =
      resources[lookup.name]?.let { Resource2Runtime(lookup.name) }

  override suspend fun diff(resource: Resource2, context: ProvisionerContext): ResourceDiff? =
      if (resource.name == "throw_exception_on_diff") {
        throw RuntimeException()
      } else if (resource.name == "force_recreate_change") {
        ResourceDiff(
            resource,
            has_changes,
            changes = listOf(ResourceDiffItem("force_recreate_change", triggersRecreate = true)),
        )
      } else {
        lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
            ?: ResourceDiff(resource, missing)
      }

  override suspend fun apply(
      resource: Resource2,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<Resource2Runtime> {
    appliedResources.add(resource.name)
    resources[resource.name] = resource
    return ApplyResult(lookup(resource.asLookup(), context))
  }

  val destroyedResources = mutableListOf<String>()

  val appliedResources = mutableListOf<String>()

  fun isDestroyed(name: String) = destroyedResources.contains(name)

  fun isApplied(name: String) = appliedResources.contains(name)

  override suspend fun destroy(
      resource: Resource2,
      context: ProvisionerContext,
      logContext: LogContext,
  ): Boolean {
    destroyedResources.add(resource.name)
    return true
  }

  override val supportedLookupType = Resource2Lookup::class

  override val supportedResourceType = Resource2::class
}
