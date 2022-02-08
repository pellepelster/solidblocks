package de.solidblocks.cloud

import de.solidblocks.base.services.ServicesManagerFactoryRegistry
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import de.solidblocks.cloud.services.ServicesManager
import de.solidblocks.cloud.status.StatusManager
import de.solidblocks.cloud.tenants.TenantsManager
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.service.helloworld.backend.HelloWorldServiceManagerFactory
import org.jooq.DSLContext

class ManagersContext(val dsl: DSLContext, val repositories: RepositoriesContext, development: Boolean) {
    val clouds = CloudsManager(repositories.clouds, repositories.environments, repositories.users, development)
    val users = UsersManager(dsl, repositories.users)
    val environments = EnvironmentsManager(dsl, clouds, repositories.environments, users, development)
    val tenants = TenantsManager(dsl, environments, repositories.tenants, users, development)
    val status = StatusManager(repositories.status, repositories.environments)
    val services: ServicesManager

    init {
        val registry = ServicesManagerFactoryRegistry()
        registry.addFactory(HelloWorldServiceManagerFactory())
        services = ServicesManager(repositories.services, users, registry)
    }
}
