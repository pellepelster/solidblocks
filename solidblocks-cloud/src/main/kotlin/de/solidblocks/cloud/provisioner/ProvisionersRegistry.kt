package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

class ProvisionersRegistry(
    val resourceLookupProviders: List<ResourceLookupProvider<*, *>> = emptyList(),
    val resourceProvisioners: List<InfrastructureResourceProvisioner<*, *>> = emptyList(),
) {

    private val logger = KotlinLogging.logger {}

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(
        lookup: ResourceLookupType,
        context: ProvisionerContext,
    ): RuntimeType? = runBlocking {
        val provider =
            resourceLookupProviders.firstOrNull {
                it.supportedLookupType.java.isAssignableFrom(lookup::class.java)
            }
        if (provider == null) {
            throw RuntimeException("no lookup provider found for '${lookup::class.qualifiedName}'")
        }

        @Suppress("UNCHECKED_CAST")
        (provider as ResourceLookupProvider<InfrastructureResourceLookup<RuntimeType>, RuntimeType>).lookup(lookup,context,)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <ResourceType : BaseResource> provisioner(
        resource: ResourceType
    ): InfrastructureResourceProvisioner<ResourceType, *> =
        provisioner(resource::class.java) as InfrastructureResourceProvisioner<ResourceType, *>

    private fun <ResourceType : BaseResource> provisioner(
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

        @Suppress("UNCHECKED_CAST")
        return provisioner as InfrastructureResourceProvisioner<ResourceType, *>
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <ResourceType : BaseResource, RuntimeType : BaseInfrastructureResourceRuntime> apply(
        resource: ResourceType,
        context: ProvisionerContext,
        log: LogContext,
    ): ApplyResult<RuntimeType> =
        provisioner(resource).apply(resource, context, log) as ApplyResult<RuntimeType>

    suspend fun <ResourceType : BaseResource, RuntimeType : BaseInfrastructureResourceRuntime> diff(
        resource: ResourceType,
        context: ProvisionerContext,
    ): ResourceDiff? = provisioner(resource).diff(resource, context)

    suspend fun <ResourceType : BaseResource, RuntimeType : BaseInfrastructureResourceRuntime> help(
        resource: ResourceType,
        context: ProvisionerContext,
    ): List<InfrastructureResourceHelp> = provisioner(resource).help(resource, context)

    suspend fun <ResourceType : BaseResource> destroy(
        resource: ResourceType,
        context: ProvisionerContext,
        logContext: LogContext,
    ): Boolean = provisioner(resource).destroy(resource, context, logContext)

    @Suppress("UNCHECKED_CAST")
    suspend fun <ResourceType : BaseResource, RuntimeType : BaseInfrastructureResourceRuntime> list(
        resourceType: Class<ResourceType>
    ) = provisioner(resourceType).list() as List<RuntimeType>
}
