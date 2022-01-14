package de.solidblocks.cloud.users

import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.model.UsersRepository
import mu.KotlinLogging
import org.bouncycastle.crypto.generators.BCrypt
import java.security.SecureRandom

class UsersManager(val usersRepository: UsersRepository) {

    private val logger = KotlinLogging.logger {}

    val random = SecureRandom()

    fun createUser(reference: TenantReference, email: String, password: String): Boolean {
        val userReference = reference.toUser(email)

        logger.info { "creating user '$email'" }

        val salt = ByteArray(16)
        random.nextBytes(salt)

        // TODO raise cost index
        val encryptedPassword = BCrypt.generate(password.toByteArray(), salt, 4)
        return usersRepository.create(email, encryptedPassword.toString(), salt.toString())
    }
}
