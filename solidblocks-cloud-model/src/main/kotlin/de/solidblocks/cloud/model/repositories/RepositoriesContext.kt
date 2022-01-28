package de.solidblocks.cloud.model.repositories

import org.jooq.DSLContext

class RepositoriesContext(dsl: DSLContext) {

    val clouds = CloudsRepository(dsl)
    val environments = EnvironmentsRepository(dsl, clouds)
    val tenants = TenantsRepository(dsl, environments)
    val users = UsersRepository(dsl, clouds, environments, tenants)
    val services = ServicesRepository(dsl, tenants)
}
