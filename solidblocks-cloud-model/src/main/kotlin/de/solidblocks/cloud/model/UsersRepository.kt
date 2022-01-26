package de.solidblocks.cloud.model

import de.solidblocks.base.resources.CloudResource
import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.base.resources.TenantResource
import de.solidblocks.cloud.model.entities.UserEntity
import de.solidblocks.config.db.tables.references.USERS
import mu.KotlinLogging
import org.jooq.DSLContext
import java.util.*

class UsersRepository(val dsl: DSLContext, val cloudRepository: CloudRepository, val environmentRepository: EnvironmentRepository, val tenantRepository: TenantRepository) {

    private val logger = KotlinLogging.logger {}

    fun createAdminUser(email: String, password: String, salt: String): Boolean {
        return createUser(email = email, admin = true, password = password, salt = salt)
    }

    fun createCloudUser(reference: CloudResource, email: String, password: String, salt: String): Boolean {
        val cloud = cloudRepository.getCloud(reference) ?: return false
        return createUser(cloud = cloud.id, email = email, admin = false, password = password, salt = salt)
    }

    fun createEnvironmentUser(reference: EnvironmentResource, email: String, password: String, salt: String): Boolean {
        val environment = environmentRepository.getEnvironment(reference) ?: return false
        return createUser(environment = environment.id, email = email, admin = false, password = password, salt = salt)
    }

    fun createTenantUser(reference: TenantResource, email: String, password: String, salt: String): Boolean {
        val tenant = tenantRepository.getTenant(reference) ?: return false
        return createUser(tenant = tenant.id, email = email, admin = false, password = password, salt = salt)
    }

    private fun createUser(cloud: UUID? = null, environment: UUID? = null, tenant: UUID? = null, email: String, admin: Boolean, password: String, salt: String): Boolean {
        logger.info { "creating user '$email'" }
        val id = UUID.randomUUID()

        dsl.insertInto(USERS).columns(
            USERS.ID,
            USERS.CLOUD,
            USERS.ENVIRONMENT,
            USERS.TENANT,
            USERS.EMAIL,
            USERS.DELETED,
            USERS.ADMIN,
            USERS.PASSWORD,
            USERS.SALT,
        ).values(id, cloud, environment, tenant, email, false, admin, password, salt).execute()

        return true
    }

    fun hasUser(email: String) = dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetchOptional().isPresent

    fun getUser(email: String): UserEntity? {

        val users = dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetch()

        if (users.isEmpty()) {
            logger.warn { "user '$email' does not exist" }
            return null
        }

        val user = users.first()

        if (user.cloud != null) {
            val cloud = cloudRepository.getCloud(user.cloud!!)
            return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, cloud = cloud)
        }

        if (user.environment != null) {
            val environment = environmentRepository.getEnvironment(user.environment!!)
            return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, environment = environment)
        }

        if (user.tenant != null) {
            val tenant = tenantRepository.getTenant(user.tenant!!)
            return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, tenant = tenant)
        }

        return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, admin = true)
    }
}
