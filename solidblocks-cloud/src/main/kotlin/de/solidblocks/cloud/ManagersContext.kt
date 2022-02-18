package de.solidblocks.cloud

import de.solidblocks.base.services.ServicesManagerFactoryRegistry
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import de.solidblocks.cloud.services.ServicesManager
import de.solidblocks.cloud.tenants.TenantsManager
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.service.helloworld.backend.HelloWorldServiceManagerFactory
import org.jooq.DSLContext

class ManagersContext(
    val dsl: DSLContext,
    val repositories: RepositoriesContext,
    schedulerContext: SchedulerContext,
    development: Boolean
) {
    val users = UsersManager(dsl, repositories.users, repositories.clouds)
    val clouds = CloudsManager(dsl, repositories.clouds, repositories.environments, users, development)
    val environments = EnvironmentsManager(dsl, clouds, repositories.environments, repositories.tenants, schedulerContext, users)
    val tenants = TenantsManager(dsl, environments, repositories.tenants, repositories.services, schedulerContext, users, development)
    val services: ServicesManager

    init {
        val registry = ServicesManagerFactoryRegistry()
        registry.addFactory(HelloWorldServiceManagerFactory())
        services = ServicesManager(repositories.services, users, registry)
    }
}
