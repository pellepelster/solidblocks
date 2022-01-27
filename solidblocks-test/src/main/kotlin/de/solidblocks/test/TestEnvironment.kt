package de.solidblocks.test

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.UserReference
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

    val reference = UserReference("cloud1", "environment1", "tenant1", "user1")

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
        environmentsManager =
            EnvironmentsManager(database.dsl, cloudRepository, environmentRepository, usersManager, true)
        tenantsManager =
            TenantsManager(database.dsl, cloudRepository, environmentsManager, tenantRepository, usersManager, true)
    }

    fun createCloud(cloud: String = "cloud1", rootDomain: String = "dev.local"): Boolean {
        cloudRepository.createCloud(cloud, rootDomain)
        return true
    }

    fun createEnvironment(
        cloud: String = "cloud1",
        environment: String = "environment1",
        email: String = "juergen@$environment.$cloud",
        password: String = "password1"
    ): Boolean {
        val reference = EnvironmentReference(cloud, environment)

        return environmentsManager.create(
            reference,
            reference.environment,
            email,
            password,
            "<none>",
            "<none>",
            "<none>",
            "<none>"
        ) ?: throw RuntimeException()
    }

    fun createTenant(
        cloud: String = "cloud1",
        environment: String = "environment1",
        tenant: String = "tenant1",
    ) = tenantRepository.createTenant(EnvironmentReference(cloud, environment), tenant, "<none>>") != null
}
