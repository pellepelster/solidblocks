package de.solidblocks.test

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.UserReference
import de.solidblocks.cloud.ManagersContext
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.toReference
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import org.jooq.DSLContext

class TestEnvironment {

    val database: SolidblocksDatabase
    val repositories: RepositoriesContext
    val managers: ManagersContext

    val reference = UserReference("cloud1", "environment1", "tenant1", "user1")

    val dsl: DSLContext
        get() = database.dsl

    init {
        database = SolidblocksDatabase(TestConstants.TEST_DB_JDBC_URL())
        database.ensureDBSchema()

        repositories = RepositoriesContext(database.dsl)
        managers = ManagersContext(database.dsl, repositories, true)
    }

    fun createCloud(cloud: String = "cloud1", rootDomain: String = "dev.local") =
        managers.clouds.createCloud(cloud, rootDomain) ?: throw RuntimeException()

    fun createCloudAndEnvironment(cloud: String, environment: String): EnvironmentReference? {
        val reference = createCloud(cloud)
        return createEnvironment(reference.toReference(), environment).toReference()
    }

    fun createEnvironment(
        cloud: String,
        environment: String = "env1",
        email: String = "juergen@$environment.${reference.cloud}",
        password: String = "password1"
    ): EnvironmentEntity {
        val cloudRef = createCloud(cloud).toReference()
        return createEnvironment(cloudRef, environment, email, password)
    }

    fun createEnvironment(
        reference: CloudReference,
        environment: String = "env1",
        email: String = "juergen@$environment.${reference.cloud}",
        password: String = "password1"
    ): EnvironmentEntity {
        return managers.environments.create(
            reference,
            environment,
            email,
            password,
            "<none>",
            "<none>",
            "<none>",
            "<none>"
        ) ?: throw RuntimeException()
    }

    fun createTenant(
        reference: EnvironmentReference,
        tenant: String,
        email: String,
        password: String = "password1"
    ) {
        managers.tenants.create(reference, tenant, email, password)
    }

    fun createTenant(
        cloud: String,
        environment: String,
        tenant: String,
        email: String = "juergen@$tenant.$environment.${reference.cloud}",
        password: String = "password1"
    ): Pair<String, String> {
        val cloudRef = createCloud(cloud).toReference()
        val environmentRef = createEnvironment(cloudRef, environment).toReference()
        createTenant(environmentRef, tenant, email, password)

        return email to password
    }
}
