package de.solidblocks.cloud

import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import de.solidblocks.cloud.tenants.TenantsManager
import de.solidblocks.cloud.users.UsersManager
import org.jooq.DSLContext

class ManagersContext(val dsl: DSLContext, val repositories: RepositoriesContext, development: Boolean) {
    val clouds = CloudsManager(repositories.clouds, repositories.environments, repositories.users, development)
    val users = UsersManager(dsl, repositories.users)
    val environments = EnvironmentsManager(dsl, clouds, repositories.environments, users, development)
    val tenants = TenantsManager(dsl, environments, repositories.tenants, users, development)
}
