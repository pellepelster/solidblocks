package de.solidblocks.test

import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.base.resources.UserResource
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.*
import de.solidblocks.cloud.tenants.TenantsManager
import de.solidblocks.cloud.users.UsersManager
import org.jooq.DSLContext

class TestEnvironment {

    private val database: SolidblocksDatabase

    val cloudRepository: CloudRepository
    val environmentRepository: EnvironmentRepository
    val tenantRepository: TenantRepository
    val usersRepository: UsersRepository

    val cloudsManager: CloudsManager
    val environmentsManager: EnvironmentsManager
    val tenantsManager: TenantsManager
    val usersManager: UsersManager

    val reference = UserResource("cloud1", "environment1", "tenant1", "user1")

    val dsl: DSLContext
        get() = database.dsl

    init {
        database = SolidblocksDatabase(TestConstants.TEST_DB_JDBC_URL())
        database.ensureDBSchema()
        cloudRepository = CloudRepository(database.dsl)

        environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        tenantRepository = TenantRepository(database.dsl, environmentRepository)
        usersRepository = UsersRepository(database.dsl, cloudRepository, environmentRepository, tenantRepository)

        cloudsManager = CloudsManager(cloudRepository, environmentRepository, usersRepository, true)
        usersManager = UsersManager(database.dsl, usersRepository)
        environmentsManager = EnvironmentsManager(database.dsl, cloudRepository, environmentRepository, usersManager, true)
        tenantsManager = TenantsManager(database.dsl, cloudRepository, environmentsManager, tenantRepository, usersManager, true)
    }

    fun createCloud(cloud: String = "cloud1", rootDomain: String = "dev.local") {
        cloudRepository.createCloud(cloud, rootDomain)
    }

    fun createEnvironment(cloud: String = "cloud1", environment: String = "environment1", email: String = "juergen@$cloud.$environment", password: String = "password1"): EnvironmentResource {
        val reference = EnvironmentResource(cloud, environment)
        environmentsManager.create(reference, reference.environment, email, password, "<none>", "<none>", "<none>", "<none>")

        return reference
    }

    fun createTenant(
        cloud: String = "cloud1",
        environment: String = "environment1",
        tenant: String = "tenant1",
    ) {
        tenantRepository.createTenant(EnvironmentResource(cloud, environment), tenant, "<none>>")
    }
}
