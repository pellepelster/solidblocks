package de.solidblocks.test

import de.solidblocks.base.UserReference
import de.solidblocks.cloud.model.*
import org.jooq.DSLContext

class TestEnvironment {

    private val database: SolidblocksDatabase

    val cloudRepository: CloudRepository
    val environmentRepository: EnvironmentRepository
    val tenantRepository: TenantRepository
    val usersRepository: UsersRepository

    val reference = UserReference("cloud1", "environment1", "tenant1", "user1")

    val dsl: DSLContext
        get() = database.dsl

    init {
        database = SolidblocksDatabase(TestConstants.TEST_DB_JDBC_URL())
        database.ensureDBSchema()
        cloudRepository = CloudRepository(database.dsl)
        environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        tenantRepository = TenantRepository(database.dsl, environmentRepository)
        usersRepository = UsersRepository(database.dsl)
    }

    fun ensurecloud() {
        cloudRepository.createCloud(reference.cloud, "dev.local")
    }

    fun ensureEnvironment() {
        ensurecloud()
        environmentRepository.createEnvironment(reference)
    }

    fun ensureTenant() {
        ensureEnvironment()
        tenantRepository.createTenant(reference, "tenant1", "<none>>")
    }
}
