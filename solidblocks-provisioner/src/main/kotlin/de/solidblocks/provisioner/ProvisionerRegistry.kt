package de.solidblocks.provisioner

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource
import de.solidblocks.core.IResourceLookup
import mu.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
open class ProvisionerRegistry(private val applicationContext: ApplicationContext) {

    private val logger = KotlinLogging.logger {}

    fun supportsResource(clazz: KClass<*>): Boolean {

        val provisioners = applicationContext.getBeansOfType(IInfrastructureResourceProvisioner::class.java).values

        return provisioners.any { it.getResourceType() == clazz.java }
    }

    fun <ResourceType : IResource, RuntimeType> provisioner(resource: ResourceType): IInfrastructureResourceProvisioner<ResourceType, RuntimeType> {
        val provisioners = applicationContext.getBeansOfType(IInfrastructureResourceProvisioner::class.java)

        val provisioner = provisioners.values.firstOrNull {
            it.getResourceType().isAssignableFrom(resource::class.java)
        }

        if (provisioner == null) {
            throw RuntimeException("no provisioner found for resource type '${resource::class.java}'")
        }

        return provisioner as IInfrastructureResourceProvisioner<ResourceType, RuntimeType>
    }

    fun <ResourceType, Type : IInfrastructureResource<ResourceType, RuntimeType>, RuntimeType> provisioner1(clazz: KClass<Type>): IInfrastructureResourceProvisioner<ResourceType, RuntimeType> {
        val provisioners = applicationContext.getBeansOfType(IInfrastructureResourceProvisioner::class.java)

        val provisioner = provisioners.values.firstOrNull {
            it.getResourceType().isAssignableFrom(clazz.java)
        }

        if (provisioner == null) {
            throw RuntimeException("no provisioner found for resource type '${clazz.java}'")
        }

        return provisioner as IInfrastructureResourceProvisioner<ResourceType, RuntimeType>
    }

    fun <LookupType : IResourceLookup<RuntimeType>, RuntimeType> datasource(lookup: LookupType): IResourceLookupProvider<IResourceLookup<RuntimeType>, RuntimeType> {
        val datasources = applicationContext.getBeansOfType(IResourceLookupProvider::class.java)

        val datasource = datasources.values.firstOrNull {
            it.getLookupType().isAssignableFrom(lookup::class.java)
        }

        if (datasource == null) {
            throw RuntimeException("no resource lookup provider found for type '${lookup::class.java}'")
        }

        return datasource as IResourceLookupProvider<IResourceLookup<RuntimeType>, RuntimeType>
    }
}
