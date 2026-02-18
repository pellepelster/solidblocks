package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class Resource1Provisioner :
    ResourceLookupProvider<Resource1Lookup, Resource1Runtime>,
    InfrastructureResourceProvisioner<Resource1, Resource1Runtime> {

  private val logger = KotlinLogging.logger {}

  val resources = mutableMapOf<String, Resource1>()

  override suspend fun lookup(lookup: Resource1Lookup, context: ProvisionerContext) =
      resources[lookup.name]?.let { Resource1Runtime(lookup.name, listOf()) }

  override suspend fun diff(resource: Resource1, context: ProvisionerContext) =
      lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
          ?: ResourceDiff(resource, missing)

  override suspend fun apply(
      resource: Resource1,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<Resource1Runtime> {
    resources[resource.name] = resource
    return ApplyResult(lookup(resource.asLookup(), context))
  }

  override val supportedLookupType = Resource1Lookup::class

  override val supportedResourceType = Resource1::class
}
