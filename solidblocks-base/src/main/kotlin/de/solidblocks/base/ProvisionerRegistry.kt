package de.solidblocks.base

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource
import de.solidblocks.core.IResourceLookup
import mu.KotlinLogging
import kotlin.reflect.KClass

open class ProvisionerRegistry {

    private val provisioners = mutableSetOf<IInfrastructureResourceProvisioner<Any, Any>>()

    private val lookupProviders = mutableSetOf<IResourceLookupProvider<IResourceLookup<Any>, Any>>()

    private val logger = KotlinLogging.logger {}

    fun supportsResource(clazz: KClass<*>): Boolean {
        return provisioners.any { it.getResourceType() == clazz.java }
    }

    fun <ResourceType : IResource, RuntimeType> provisioner(resource: ResourceType): IInfrastructureResourceProvisioner<ResourceType, RuntimeType> {
        val provisioner = provisioners.firstOrNull {
            it.getResourceType().isAssignableFrom(resource::class.java)
        }

        if (provisioner == null) {
            throw RuntimeException("no provisioner found for resource type '${resource::class.java}'")
        }

        return provisioner as IInfrastructureResourceProvisioner<ResourceType, RuntimeType>
    }

    fun <ResourceType, Type : IInfrastructureResource<ResourceType, RuntimeType>, RuntimeType> provisioner1(clazz: KClass<Type>): IInfrastructureResourceProvisioner<ResourceType, RuntimeType> {
        val provisioner = provisioners.firstOrNull {
            it.getResourceType().isAssignableFrom(clazz.java)
        }

        if (provisioner == null) {
            throw RuntimeException("no provisioner found for resource type '${clazz.java}'")
        }

        return provisioner as IInfrastructureResourceProvisioner<ResourceType, RuntimeType>
    }

    fun <LookupType : IResourceLookup<RuntimeType>, RuntimeType> datasource(lookup: LookupType): IResourceLookupProvider<IResourceLookup<RuntimeType>, RuntimeType> {
        val datasource = lookupProviders.firstOrNull {
            it.getLookupType().isAssignableFrom(lookup::class.java)
        }

        if (datasource == null) {
            throw RuntimeException("no resource lookup provider found for type '${lookup::class.java}'")
        }

        return datasource as IResourceLookupProvider<IResourceLookup<RuntimeType>, RuntimeType>
    }

    fun addProvisioner(provisioner: IInfrastructureResourceProvisioner<Any, Any>) {
        provisioners.add(provisioner)

        if (provisioner is IResourceLookupProvider<*, *>) {
            addLookupProvider(provisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)
        }
    }

    fun addLookupProvider(lookupProvider: IResourceLookupProvider<IResourceLookup<Any>, Any>) {
        lookupProviders.add(lookupProvider)
    }
}
