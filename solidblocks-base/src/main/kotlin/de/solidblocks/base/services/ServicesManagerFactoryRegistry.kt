package de.solidblocks.base.services

import de.solidblocks.api.services.ServiceCatalogResponse
import de.solidblocks.api.services.ServiceManagerFactory

class ServicesManagerFactoryRegistry {

    private val serviceFactories = mutableListOf<ServiceManagerFactory<*>>()

    fun getByType(type: String) = serviceFactories.firstOrNull { it.type == type }

    fun addFactory(serviceManagerFactory: ServiceManagerFactory<*>) {
        serviceFactories.add(serviceManagerFactory)
    }

    fun serviceCatalog() = ServiceCatalogResponse(serviceFactories.map { it.catalogItem })
}
