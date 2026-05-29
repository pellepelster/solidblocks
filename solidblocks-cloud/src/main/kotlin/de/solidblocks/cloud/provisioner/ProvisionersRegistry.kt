package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.DestroyableResourceProvisioner
import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ListableResourceLookupProvider
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

    private val provisionersByResourceType by lazy { resourceProvisioners.indexByUnique { it.supportedResourceType } }

    private val provisionersByLookupType by lazy { resourceProvisioners.indexByUnique { it.supportedLookupType } }

    private val lookupProvidersByType by lazy {
        (resourceProvisioners.filterIsInstance<InfrastructureResourceLookupProvider<*, *>>() + resourceLookupProviders)
            .distinctBy { it::class }
            .indexByUnique { it.supportedLookupType }
    }

    private fun <V> List<V>.indexByUnique(key: (V) -> KClass<*>): Map<KClass<*>, V> {
        val grouped = this.groupBy(key)
        grouped.forEach { (clazz, entries) ->
            if (entries.size > 1) {
                throw RuntimeException("expected one but found ${entries.size} instances for '${clazz.qualifiedName}'")
            }
        }
        return grouped.mapValues { it.value.single() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun provisioner(resource: BaseResource): InfrastructureResourceProvisioner<BaseResource, BaseInfrastructureResourceRuntime, InfrastructureResourceLookup<*>> {
        val provisioner = provisionersByResourceType[resource::class] ?: provisionersByLookupType[resource::class]
            ?: throw RuntimeException("no provisioner found for '${resource::class.qualifiedName}'")

        return provisioner as InfrastructureResourceProvisioner<BaseResource, BaseInfrastructureResourceRuntime, InfrastructureResourceLookup<*>>
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> apply(resource: BaseResource, context: ProvisionerApplyContext, log: LogContext): Result<RuntimeType> {
        val provisioner = provisioner(resource)
        logger.info {
            "creating ${resource.logText()} using provisioner ${provisioner::class.qualifiedName}"
        }

        return provisioner.apply(resource, context, log) as Result<RuntimeType>
    }

    suspend fun <ResourceType : BaseResource> diff(resource: ResourceType, context: ProvisionerDiffContext): ResourceDiff? = provisioner(resource).diff(resource, context)

    @Suppress("UNCHECKED_CAST")
    suspend fun <LookupType : InfrastructureResourceLookup<*>> destroy(lookup: LookupType, context: SSHProvisionerContext, log: LogContext): Boolean {
        val provisioner = provisioner(lookup)
        return if (provisioner is DestroyableResourceProvisioner<*>) {
            (provisioner as DestroyableResourceProvisioner<InfrastructureResourceLookup<*>>).destroy(lookup, context, log)
        } else {
            logger.warn { "${lookup.logText()} does not support destroy (${provisioner::class.qualifiedName})" }
            false
        }
    }

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType, context: SSHProvisionerContext): RuntimeType? = runBlocking {
        val provider = lookupProvidersByType[lookup::class]
            ?: throw RuntimeException("no lookup found for '${lookup::class.qualifiedName}'")

        @Suppress("UNCHECKED_CAST")
        (provider as InfrastructureResourceLookupProvider<ResourceLookupType, BaseInfrastructureResourceRuntime>).lookup(lookup, context) as RuntimeType?
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <LookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>): List<LookupType> {
        val provider = lookupProvidersByType[clazz]
            ?: throw RuntimeException("no lookup provider found for '$clazz'")

        if (provider !is ListableResourceLookupProvider<*>) {
            throw RuntimeException("lookup provider for '$clazz' does not support list")
        }

        return (provider as ListableResourceLookupProvider<LookupType>).list()
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
