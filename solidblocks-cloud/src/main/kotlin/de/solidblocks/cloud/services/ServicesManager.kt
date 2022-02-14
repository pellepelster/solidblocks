package de.solidblocks.cloud.services

import de.solidblocks.base.services.ServicesManagerFactoryRegistry
import de.solidblocks.cloud.model.repositories.ServicesRepository
import de.solidblocks.cloud.tenants.api.toReference
import de.solidblocks.cloud.users.UsersManager
import mu.KotlinLogging

class ServicesManager(private val servicesRepository: ServicesRepository, private val usersManager: UsersManager, private val servicesManagerFactoryRegistry: ServicesManagerFactoryRegistry) {

    private val logger = KotlinLogging.logger {}

    private val services = mutableListOf<ServiceInstance>()

    init {
        for (service in servicesRepository.allServices()) {

            val factory = servicesManagerFactoryRegistry.getByType(service.type)
            if (factory == null) {
                logger.error { "no factory found for service type '${service.type}'" }
                continue
            }

            logger.error { "creating service manager for service '${service.name}' (${service.id})" }
            services.add(ServiceInstance(service, factory.createServiceManager()))
        }
    }

    fun services(email: String): List<ServiceInstance> {
        val user = usersManager.getUser(email) ?: return emptyList()

        return services.filter {
            if (user.tenant == null) {
                false
            } else {
                it.service.tenant.id == user.tenant!!.id
            }
        }
    }

    fun create(email: String, name: String, type: String): ServiceInstance? {
        val user = usersManager.getUser(email) ?: return null

        if (user.tenant == null) {
            logger.warn { "not creating service for user '$email' without tenant" }
            return null
        }

        val factory = servicesManagerFactoryRegistry.getByType(type)
        if (factory == null) {
            logger.error { "no factory found for service type '$type'" }
            return null
        }

        val service = servicesRepository.createService(user.tenant!!.toReference(), name, type)
        if (service == null) {
            logger.warn { "failed to create service '$name' for user '$email'" }
        }

        val instance = ServiceInstance(service!!, factory.createServiceManager())
        services.add(instance)

        return instance
    }

    fun serviceCatalog() = servicesManagerFactoryRegistry.serviceCatalog()
}
