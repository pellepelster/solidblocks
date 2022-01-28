package de.solidblocks.cloud.model.repositories

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cloud.model.entities.UserEntity
import de.solidblocks.config.db.tables.references.USERS
import mu.KotlinLogging
import org.jooq.DSLContext
import java.util.*

class UsersRepository(val dsl: DSLContext, val cloudsRepository: CloudsRepository, val environmentsRepository: EnvironmentsRepository, val tenantsRepository: TenantsRepository) {

    private val logger = KotlinLogging.logger {}

    fun createAdminUser(email: String, password: String, salt: String): Boolean {
        return createUser(email = email, admin = true, password = password, salt = salt)
    }

    fun createCloudUser(reference: CloudReference, email: String, password: String, salt: String): Boolean {
        val cloud = cloudsRepository.getCloud(reference) ?: return false
        return createUser(cloud = cloud.id, email = email, admin = false, password = password, salt = salt)
    }

    fun createEnvironmentUser(reference: EnvironmentReference, email: String, password: String, salt: String): Boolean {
        val environment = environmentsRepository.getEnvironment(reference) ?: return false

        return createUser(environment = environment.id, email = email, admin = false, password = password, salt = salt)
    }

    fun createTenantUser(reference: TenantReference, email: String, password: String, salt: String): Boolean {
        val tenant = tenantsRepository.getTenant(reference) ?: return false
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
            val cloud = cloudsRepository.getCloud(user.cloud!!)
            return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, cloud = cloud)
        }

        if (user.environment != null) {
            val environment = environmentsRepository.getEnvironment(user.environment!!)
            return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, environment = environment)
        }

        if (user.tenant != null) {
            val tenant = tenantsRepository.getTenant(user.tenant!!)
            return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, tenant = tenant)
        }

        return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!, admin = true)
    }
}
