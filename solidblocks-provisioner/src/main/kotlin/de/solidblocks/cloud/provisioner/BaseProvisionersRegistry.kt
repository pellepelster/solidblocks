package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.lookup.BaseResourceLookupProvider
import de.solidblocks.cloud.api.lookup.ListableResourceLookupProvider
import de.solidblocks.cloud.api.provisioner.BaseDestroyableResourceProvisioner
import de.solidblocks.cloud.api.provisioner.BaseResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

abstract class BaseProvisionersRegistry<DiffContextType, ApplyContextType, DestroyContextType, LookupContextType>(
    val resourceLookupProviders: List<BaseResourceLookupProvider<InfrastructureResourceLookup<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, LookupContextType>> = emptyList(),
    val resourceProvisioners: List<BaseResourceProvisioner<BaseInfrastructureResource<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, DiffContextType, ApplyContextType>> = emptyList(),
) {
    private val logger = KotlinLogging.logger {}

    private val provisionersByResourceType by lazy { resourceProvisioners.indexByUnique { it.supportedResourceType } }

    private val provisionersByLookupType by lazy { resourceProvisioners.indexByUnique { it.supportedLookupType } }

    private val lookupProvidersByType by lazy {
        (
            resourceProvisioners.filterIsInstance<BaseResourceLookupProvider<InfrastructureResourceLookup<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, LookupContextType>>() +
                resourceLookupProviders
            )
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
    private fun provisioner(
        resource: BaseResource,
    ): BaseResourceProvisioner<BaseInfrastructureResource<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, DiffContextType, ApplyContextType> {
        val provisioner = provisionersByResourceType[resource::class] ?: provisionersByLookupType[resource::class]
            ?: throw RuntimeException("no provisioner found for '${resource::class.qualifiedName}'")

        return provisioner
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <RuntimeType : BaseInfrastructureResourceRuntime, ResourceType : BaseInfrastructureResource<RuntimeType>> apply(resource: ResourceType, context: ApplyContextType): Result<RuntimeType> {
        val provisioner = provisioner(resource)
        logger.info {
            "creating ${resource.logText()} using provisioner ${provisioner::class.qualifiedName}"
        }

        return provisioner.apply(resource as BaseInfrastructureResource<BaseInfrastructureResourceRuntime>, context) as Result<RuntimeType>
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <ResourceType : BaseInfrastructureResource<*>> diff(resource: ResourceType, context: DiffContextType): Result<ResourceDiff> =
        provisioner(resource).diff(resource as BaseInfrastructureResource<BaseInfrastructureResourceRuntime>, context)

    @Suppress("UNCHECKED_CAST")
    suspend fun <LookupType : InfrastructureResourceLookup<*>> destroy(lookup: LookupType, context: DestroyContextType): Boolean {
        val provisioner = provisioner(lookup)
        return if (provisioner is BaseDestroyableResourceProvisioner<*, *>) {
            (provisioner as BaseDestroyableResourceProvisioner<InfrastructureResourceLookup<*>, DestroyContextType>).destroy(
                lookup,
                context,
            )
        } else {
            logger.warn { "${lookup.logText()} does not support destroy (${provisioner::class.qualifiedName})" }
            false
        }
    }

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType, context: LookupContextType): RuntimeType? = runBlocking {
        val provider = lookupProvidersByType[lookup::class]
            ?: throw RuntimeException("no lookup found for '${lookup::class.qualifiedName}'")

        @Suppress("UNCHECKED_CAST")
        provider.lookup(
            lookup as InfrastructureResourceLookup<BaseInfrastructureResourceRuntime>,
            context,
        ) as RuntimeType?
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
}
