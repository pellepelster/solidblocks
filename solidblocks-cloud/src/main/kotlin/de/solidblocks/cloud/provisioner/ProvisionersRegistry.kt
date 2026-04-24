package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.managerForRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

class ProvisionersRegistry(val resourceLookupProviders: List<ResourceLookupProvider<*, *>> = emptyList(), val resourceProvisioners: List<InfrastructureResourceProvisioner<*, *>> = emptyList()) {

    private val logger = KotlinLogging.logger {}

    @Suppress("UNCHECKED_CAST")
    private fun <ResourceType : BaseResource> provisioner(resource: ResourceType): InfrastructureResourceProvisioner<ResourceType, *> =
        provisioner(resource::class.java) as InfrastructureResourceProvisioner<ResourceType, *>

    private fun <ResourceType : BaseResource> provisioner(resourceType: Class<ResourceType>): InfrastructureResourceProvisioner<ResourceType, *> {
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
    suspend fun <ResourceType : BaseResource, RuntimeType : BaseInfrastructureResourceRuntime> apply(resource: ResourceType, context: ProvisionerApplyContext, log: LogContext): Result<RuntimeType> {
        val provisioner = provisioner(resource)
        logger.info {
            "creating ${resource.logText()} using provisioner ${provisioner::class.qualifiedName}"
        }
        return provisioner.apply(resource, context, log) as Result<RuntimeType>
    }

    suspend fun <ResourceType : BaseResource> diff(resource: ResourceType, context: ProvisionerDiffContext): ResourceDiff? = provisioner(resource).diff(resource, context)

    suspend fun <ResourceType : BaseResource> destroy(resource: ResourceType, context: ProvisionerContext, log: LogContext): Boolean = provisioner(resource).destroy(resource, context, log)

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType, context: ProvisionerContext): RuntimeType? = runBlocking {
        val provider =
            resourceLookupProviders.firstOrNull {
                it.supportedLookupType.java.isAssignableFrom(lookup::class.java)
            }

        if (provider == null) {
            throw RuntimeException("no lookup provider found for '${lookup::class.qualifiedName}'")
        }

        @Suppress("UNCHECKED_CAST")
        (provider as ResourceLookupProvider<InfrastructureResourceLookup<RuntimeType>, RuntimeType>)
            .lookup(lookup, context)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>): List<RuntimeType> {
        val provider = resourceLookupProviders.firstOrNull { it.supportedLookupType == clazz }

        if (provider == null) {
            throw RuntimeException("no lookup provider found for '$clazz'")
        }

        @Suppress("UNCHECKED_CAST")
        return (
            provider
                as ResourceLookupProvider<InfrastructureResourceLookup<RuntimeType>, RuntimeType>
            )
            .list()
    }

    companion object {

        fun List<ProviderRegistration<*, *, *>>.createRegistry(providers: List<ProviderConfigurationRuntime>) = ProvisionersRegistry(this.createLookups(providers), this.createProvisioners(providers))

        fun List<ProviderRegistration<*, *, *>>.createProvisioners(providers: List<ProviderConfigurationRuntime>): List<InfrastructureResourceProvisioner<*, *>> = providers.flatMap {
            val manager:
                ProviderManager<ProviderConfiguration, ProviderConfigurationRuntime> =
                this.managerForRuntime(it)
            manager.createProvisioners(it)
        }

        fun List<ProviderRegistration<*, *, *>>.createLookups(providers: List<ProviderConfigurationRuntime>) = providers.flatMap {
            val manager:
                ProviderManager<ProviderConfiguration, ProviderConfigurationRuntime> =
                this.managerForRuntime(it)
            manager.createLookupProviders(it)
        }
    }
}
