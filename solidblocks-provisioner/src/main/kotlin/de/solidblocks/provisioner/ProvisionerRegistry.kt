package de.solidblocks.provisioner

import de.solidblocks.api.resources.infrastructure.IDataSourceLookup
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource
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

    fun <ResourceType, RuntimeType> provisioner(resource: IInfrastructureResource<ResourceType>): IInfrastructureResourceProvisioner<IInfrastructureResource<RuntimeType>, RuntimeType> {
        val provisioners = applicationContext.getBeansOfType(IInfrastructureResourceProvisioner::class.java)

        val provisioner = provisioners.values.firstOrNull {
            it.getResourceType() == resource::class.java
        }

        if (provisioner == null) {
            throw RuntimeException("no provisioner found for resource type '${resource::class.java}'")
        }

        return provisioner as IInfrastructureResourceProvisioner<IInfrastructureResource<RuntimeType>, RuntimeType>
    }

    fun <ResourceType : IInfrastructureResource<RuntimeType>, RuntimeType> provisioner(clazz: KClass<ResourceType>): IInfrastructureResourceProvisioner<IInfrastructureResource<RuntimeType>, RuntimeType> {
        val provisioners = applicationContext.getBeansOfType(IInfrastructureResourceProvisioner::class.java)

        val provisioner = provisioners.values.firstOrNull {
            it.getResourceType() == clazz.java
        }

        if (provisioner == null) {
            throw RuntimeException("no provisioner found for resource type '${clazz.java}'")
        }

        return provisioner as IInfrastructureResourceProvisioner<IInfrastructureResource<RuntimeType>, RuntimeType>
    }

    fun <DataSourceType : IDataSource<RuntimeType>, RuntimeType> datasource(clazz: KClass<DataSourceType>): IDataSourceLookup<IDataSource<RuntimeType>, RuntimeType> {
        val datasources = applicationContext.getBeansOfType(IDataSourceLookup::class.java)

        val datasource = datasources.values.firstOrNull {
            it.getDatasourceType() == clazz.java
        }

        if (datasource == null) {
            throw RuntimeException("no data source lookup found for data source type '${clazz.java}'")
        }

        return datasource as IDataSourceLookup<IDataSource<RuntimeType>, RuntimeType>
    }
}
