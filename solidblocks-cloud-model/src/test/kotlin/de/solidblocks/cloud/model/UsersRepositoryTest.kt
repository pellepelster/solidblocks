package de.solidblocks.cloud.model

import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class UsersRepositoryTest {

    @Test
    fun testCreateUser(testEnvironment: TestEnvironment) {
        testEnvironment.ensureTenant()

        val repository = UsersRepository(testEnvironment.dsl)

        assertThat(repository.read("user1")).isNull()
        assertThat(repository.create("user1", "password1", "salt1")).isTrue

        val user = repository.read("user1")
        assertThat(user).isNotNull
        assertThat(user?.email).isEqualTo("user1")
        assertThat(user?.password).isEqualTo("password1")
        assertThat(user?.salt).isEqualTo("salt1")
    }
}
