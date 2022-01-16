package de.solidblocks.test

import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.base.resources.UserResource
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.*
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

        cloudsManager = CloudsManager(cloudRepository, environmentRepository, true)
        usersManager = UsersManager(database.dsl, usersRepository)
        environmentsManager = EnvironmentsManager(database.dsl, cloudRepository, environmentRepository, usersManager, true)
    }

    fun ensureCloud(rootDomain: String = "dev.local") {
        cloudRepository.createCloud(reference.cloud, rootDomain)
    }

    fun createEnvironment(cloud: String = "cloud1", environment: String = "environment1", email: String = "juergen@test.local", password: String = "password1"): EnvironmentResource {
        val reference = EnvironmentResource(cloud, environment)

        cloudsManager.createCloud(reference.cloud, "dev.local")
        environmentsManager.create(reference, reference.environment, email, password, "<none>", "<none>", "<none>", "<none>")

        return reference
    }

    fun ensureTenant() {
        tenantRepository.createTenant(reference, "tenant1", "<none>>")
    }
}
