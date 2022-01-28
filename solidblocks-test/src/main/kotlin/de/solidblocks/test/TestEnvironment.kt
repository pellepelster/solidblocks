package de.solidblocks.test

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.UserReference
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.RepositoriesContext
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.entities.toReference
import de.solidblocks.cloud.tenants.TenantsManager
import de.solidblocks.cloud.users.UsersManager
import org.jooq.DSLContext

class TestEnvironment {

    val database: SolidblocksDatabase

    val repositories: RepositoriesContext

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

        repositories = RepositoriesContext(database.dsl)

        cloudsManager = CloudsManager(repositories.clouds, repositories.environments, repositories.users, true)
        usersManager = UsersManager(database.dsl, repositories.users)
        environmentsManager =
            EnvironmentsManager(database.dsl, repositories.clouds, repositories.environments, usersManager, true)
        tenantsManager =
            TenantsManager(
                database.dsl,
                repositories.clouds,
                environmentsManager,
                repositories.tenants,
                usersManager,
                true
            )
    }

    fun createCloud(cloud: String = "cloud1", rootDomain: String = "dev.local") =
        repositories.clouds.createCloud(cloud, rootDomain).toReference()

    fun createCloudAndEnvironment(cloud: String): EnvironmentReference {
        val reference = createCloud(cloud)
        return createEnvironment(cloud = reference.cloud)
    }

    fun createEnvironment(
        cloud: String = "cloud1",
        environment: String = "environment1",
        email: String = "juergen@$environment.$cloud",
        password: String = "password1"
    ): EnvironmentReference {
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
    ) = repositories.tenants.createTenant(EnvironmentReference(cloud, environment), tenant, "<none>>")?.toReference()
}
