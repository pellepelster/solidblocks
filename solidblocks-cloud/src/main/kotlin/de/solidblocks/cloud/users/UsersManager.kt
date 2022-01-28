package de.solidblocks.cloud.users

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cloud.model.entities.UserEntity
import de.solidblocks.cloud.model.repositories.UsersRepository
import mu.KotlinLogging
import org.bouncycastle.crypto.generators.BCrypt
import org.jooq.DSLContext
import org.jooq.TransactionalCallable
import java.security.SecureRandom
import java.util.*

class UsersManager(val dsl: DSLContext, val usersRepository: UsersRepository) {

    private val logger = KotlinLogging.logger {}

    // TODO raise cost index
    private val BCRYPT_COST = 4

    private val random = SecureRandom()

    private val encoder = Base64.getEncoder()

    private val decoder = Base64.getDecoder()

    fun generatePasswordAndSalt(password: String): Pair<String, String> {
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val encryptedPassword = BCrypt.generate(password.toByteArray(), salt, BCRYPT_COST)
        return encoder.encodeToString(encryptedPassword) to encoder.encodeToString(salt)
    }

    fun ensureAdminUser(email: String, password: String) = dsl.transactionResult(
        TransactionalCallable {
            if (!usersRepository.hasUser(email)) {
                val credentials = generatePasswordAndSalt(password)
                usersRepository.createAdminUser(email, credentials.first, credentials.second)
            }

            true
        }
    )

    fun createEnvironmentUser(reference: EnvironmentReference, email: String, password: String): Boolean {
        logger.info { "creating user '$email' for environment '${reference.environment}'" }

        val credentials = generatePasswordAndSalt(password)
        return usersRepository.createEnvironmentUser(reference, email, credentials.first, credentials.second)
    }

    fun createTenantUser(reference: TenantReference, email: String, password: String): Boolean {
        logger.info { "creating user '$email' for tenant '${reference.tenant}'" }

        val credentials = generatePasswordAndSalt(password)
        return usersRepository.createTenantUser(reference, email, credentials.first, credentials.second)
    }

    fun loginUser(email: String, passwordToCheck: String): UserEntity? {
        val user = usersRepository.getUser(email) ?: return null

        val encryptedPassword = decoder.decode(user.password)
        val salt = decoder.decode(user.salt)

        val encryptedPasswordToCheck = BCrypt.generate(passwordToCheck.toByteArray(), salt, BCRYPT_COST)

        if (!encryptedPassword.contentEquals(encryptedPasswordToCheck)) {
            // TODO(pelle) exponential backoff
            logger.warn { "login failed for user '$email'" }
            return null
        }

        logger.info { "login succeeded for user '$email'" }

        return user
    }

    fun getUser(email: String) = usersRepository.getUser(email)

    fun hasUser(email: String) = usersRepository.hasUser(email)
}
