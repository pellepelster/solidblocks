package de.solidblocks.cloud.model

import de.solidblocks.cloud.model.entities.UserEntity
import de.solidblocks.config.db.tables.references.USERS
import mu.KotlinLogging
import org.jooq.DSLContext
import java.util.*

class UsersRepository(val dsl: DSLContext) {

    private val logger = KotlinLogging.logger {}

    fun create(
        email: String,
        password: String,
        salt: String
    ): Boolean {
        val id = UUID.randomUUID()

        // val tenant = tenantRepository.getOptional(reference) ?: return false

        dsl.insertInto(USERS)
            .columns(
                USERS.ID,
                USERS.EMAIL,
                USERS.DELETED,
                USERS.PASSWORD,
                USERS.SALT,
            )
            .values(id, email, false, password, salt).execute()

        return true
    }

    fun read(email: String): UserEntity? {
        // val tenant = tenantRepository.getOptional(reference) ?: return null

        val users = dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetch()

        if (users.isEmpty()) {
            logger.warn { "user '$email' does not exist" }
            return null
        }

        val user = users.first()

        return UserEntity(user.id!!, user.email!!, user.salt!!, user.password!!)
    }
}
