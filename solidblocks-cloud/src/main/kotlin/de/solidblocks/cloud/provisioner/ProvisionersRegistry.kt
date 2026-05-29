package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.managerForRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

class ProvisionersRegistry(val resourceLookupProviders: List<InfrastructureResourceLookupProvider<*, *>> = emptyList(), val resourceProvisioners: List<InfrastructureResourceProvisioner<*, *, *>> = emptyList()) {

    private val logger = KotlinLogging.logger {}

    @Suppress("UNCHECKED_CAST")
    private fun <ResourceType : BaseResource> provisioner(resource: ResourceType): InfrastructureResourceProvisioner<ResourceType, *, *> =
        provisioner(resource::class.java) as InfrastructureResourceProvisioner<ResourceType, *, *>

    private fun provisioner(resourceType: Class<*>): InfrastructureResourceProvisioner<*, *, *> {
        val provisioner = resourceProvisioners.singleOrNull {
            // TODO why supportedLookupType?
            it.supportedResourceType.java.isAssignableFrom(resourceType) || it.supportedLookupType.java.isAssignableFrom(resourceType)
        }

        if (provisioner == null) {
            val count = resourceProvisioners.count {
                it.supportedResourceType.java.isAssignableFrom(resourceType)
            }

            throw RuntimeException(
                "no or more than one ($count) provisioner found for '${resourceType.name}'",
            )
        }

        @Suppress("UNCHECKED_CAST")
        return provisioner
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

    suspend fun <LookupType> destroy(lookup: LookupType, context: SSHProvisionerContext, log: LogContext): Boolean {
        val provisioner = provisioner(lookup!!.javaClass) as InfrastructureResourceProvisioner<Any, Any, LookupType>
        return provisioner.destroy(lookup, context, log)
    }

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType, context: SSHProvisionerContext): RuntimeType? = runBlocking {
        val provider =
            resourceLookupProviders.firstOrNull {
                it.supportedLookupType.java.isAssignableFrom(lookup::class.java)
            }

        if (provider == null) {
            throw RuntimeException("no lookup provider found for '${lookup::class.qualifiedName}'")
        }

        @Suppress("UNCHECKED_CAST")
        (provider as InfrastructureResourceLookupProvider<InfrastructureResourceLookup<RuntimeType>, RuntimeType>)
            .lookup(lookup, context)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <LookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>): List<LookupType> {
        val provider = resourceLookupProviders.firstOrNull { it.supportedLookupType == clazz }

        if (provider == null) {
            throw RuntimeException("no lookup provider found for '$clazz'")
        }

        @Suppress("UNCHECKED_CAST")
        return (
            provider
                as InfrastructureResourceLookupProvider<LookupType, RuntimeType>
            ).list()
    }

    companion object {

        fun List<ProviderRegistration<*, *, *>>.createRegistry(providers: List<ProviderConfigurationRuntime>) = ProvisionersRegistry(this.createLookups(providers), this.createProvisioners(providers))

        fun List<ProviderRegistration<*, *, *>>.createProvisioners(providers: List<ProviderConfigurationRuntime>): List<InfrastructureResourceProvisioner<*, *, *>> = providers.flatMap {
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
