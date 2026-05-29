package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.interpolation.EnvironmentVariableInterpolationFactory
import de.solidblocks.cloud.interpolation.StringInterpolationFactory
import de.solidblocks.cloud.interpolation.StringInterpolationRegistry
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.managerForRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.joinToStringOrEmpty
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

class ProvisionersRegistry(
    val resourceLookupProviders: List<InfrastructureResourceLookupProvider<*, *>> = emptyList(),
    val resourceProvisioners: List<InfrastructureResourceProvisioner<*, *, *>> = emptyList(),
) {

    val interpolationRegistry = StringInterpolationRegistry(this.resourceProvisioners.filterIsInstance<StringInterpolationFactory>() + listOf(EnvironmentVariableInterpolationFactory()))

    private val logger = KotlinLogging.logger {}

    @Suppress("UNCHECKED_CAST")
    private fun <ResourceType : BaseResource> supportedProvisioner(resource: ResourceType): InfrastructureResourceProvisioner<BaseResource, BaseInfrastructureResourceRuntime, InfrastructureResourceLookup<*>> =
        provisioner(resource)

    private fun provisioner(resource: BaseResource): InfrastructureResourceProvisioner<BaseResource, BaseInfrastructureResourceRuntime, InfrastructureResourceLookup<*>> {
        val provisioners = resourceProvisioners.filter {
            it.supportedResourceType.java.isAssignableFrom(resource::class.java) || it.supportedLookupType.java.isAssignableFrom(resource::class.java)
        }

        val provisioner = provisioners.singleOrNull() ?: throw RuntimeException(
            "expected one provisioner but found ${provisioners.count()} for '${resource::class.java.name}' (${provisioners.joinToStringOrEmpty { "'${it::class.qualifiedName}'" }})",
        )

        @Suppress("UNCHECKED_CAST")
        return provisioner as InfrastructureResourceProvisioner<BaseResource, BaseInfrastructureResourceRuntime, InfrastructureResourceLookup<*>>
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> apply(resource: BaseResource, context: ProvisionerApplyContext, log: LogContext): Result<RuntimeType> {
        val provisionerAndResource = supportedProvisioner(resource)
        logger.info {
            "creating ${resource.logText()} using provisioner ${provisionerAndResource::class.qualifiedName}"
        }

        return provisionerAndResource.apply(resource, context, log) as Result<RuntimeType>
    }

    suspend fun <ResourceType : BaseResource> diff(resource: ResourceType, context: ProvisionerDiffContext): ResourceDiff? = supportedProvisioner(resource).diff(resource, context)

    suspend fun <LookupType : InfrastructureResourceLookup<*>> destroy(lookup: LookupType, context: SSHProvisionerContext, log: LogContext): Boolean {
        val provisionerAndResource = provisioner(lookup)
        return provisionerAndResource.destroy(lookup, context, log)
    }

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType, context: SSHProvisionerContext): RuntimeType? = runBlocking {
        val allLookups = (resourceProvisioners.filterIsInstance<InfrastructureResourceLookupProvider<*, *>>() + resourceLookupProviders).distinctBy { it::class.java }

        val lookups =
            allLookups.filter {
                it.supportedLookupType.java.isAssignableFrom(lookup::class.java)
            }

        val provider = lookups.singleOrNull() ?: throw RuntimeException(
            "expected one lookup but found ${lookups.count()} for '${lookup::class.java.name}' (${lookups.joinToStringOrEmpty(", ") { "'${it::class.qualifiedName}'" }})",
        )

        @Suppress("UNCHECKED_CAST")
        (provider as InfrastructureResourceLookupProvider<ResourceLookupType, BaseInfrastructureResourceRuntime>).lookup(lookup, context) as RuntimeType?
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
