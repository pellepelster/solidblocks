package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceHelp
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.Resource
import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

class ProvisionersRegistry(
    val lookupProviders: List<ResourceLookupProvider<*, *>> = emptyList(),
    val resourceProvisioners: List<InfrastructureResourceProvisioner<*, *>> = emptyList(),
) {

  private val logger = KotlinLogging.logger {}

  fun <RuntimeType, ResourceLookupType : ResourceLookup<RuntimeType>> lookup(
      lookup: ResourceLookupType,
      context: ProvisionerContext,
  ): RuntimeType? = runBlocking {
    val provider =
        lookupProviders.firstOrNull {
          it.supportedLookupType.java.isAssignableFrom(lookup::class.java)
        }
    if (provider == null) {
      throw RuntimeException("no lookup provider found for '${lookup::class.qualifiedName}'")
    }

    (provider as ResourceLookupProvider<ResourceLookup<RuntimeType>, RuntimeType>).lookup(
        lookup,
        context,
    )
  }

  private fun <ResourceType : Resource> provisioner(
      resource: ResourceType
  ): InfrastructureResourceProvisioner<ResourceType, *> =
      provisioner(resource::class.java) as InfrastructureResourceProvisioner<ResourceType, *>

  private fun <ResourceType : Resource> provisioner(
      resourceType: Class<ResourceType>
  ): InfrastructureResourceProvisioner<ResourceType, *> {
    val provisioner =
        resourceProvisioners.singleOrNull {
          it.supportedResourceType.java.isAssignableFrom(resourceType)
        }

    if (provisioner == null) {
      val count =
          resourceProvisioners.count {
            it.supportedResourceType.java.isAssignableFrom(resourceType)
          }
      throw RuntimeException(
          "no or more than one ($count) provisioner found for '${resourceType.name}'",
      )
    }

    return provisioner as InfrastructureResourceProvisioner<ResourceType, *>
  }

  suspend fun <ResourceType : Resource, RuntimeType : InfrastructureResourceRuntime> apply(
      resource: ResourceType,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<RuntimeType> =
      provisioner(resource).apply(resource, context, log) as ApplyResult<RuntimeType>

  suspend fun <ResourceType : Resource, RuntimeType : InfrastructureResourceRuntime> diff(
      resource: ResourceType,
      context: ProvisionerContext,
  ): ResourceDiff? = provisioner(resource).diff(resource, context)

  suspend fun <ResourceType : Resource, RuntimeType : InfrastructureResourceRuntime> help(
      resource: ResourceType,
      context: ProvisionerContext,
  ): List<InfrastructureResourceHelp> = provisioner(resource).help(resource, context)

  suspend fun <ResourceType : Resource> destroy(
      resource: ResourceType,
      context: ProvisionerContext,
      logContext: LogContext,
  ): Boolean = provisioner(resource).destroy(resource, context, logContext)

  suspend fun <ResourceType : Resource, RuntimeType : InfrastructureResourceRuntime> list(
      resourceType: Class<ResourceType>
  ) = provisioner(resourceType).list() as List<RuntimeType>
}
