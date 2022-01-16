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
        val reference = testEnvironment.createEnvironment("cloud1", "env1")

        val repository = UsersRepository(testEnvironment.dsl, testEnvironment.cloudRepository, testEnvironment.environmentRepository, testEnvironment.tenantRepository)

        assertThat(repository.getUser("user1")).isNull()
        assertThat(repository.createEnvironmentUser(reference, "juergen2@test.local", "password2", "salt2")).isTrue

        val user = repository.getUser("juergen2@test.local")
        assertThat(user).isNotNull
        assertThat(user?.email).isEqualTo("juergen2@test.local")
        assertThat(user?.password).isEqualTo("password2")
        assertThat(user?.salt).isEqualTo("salt2")
    }
}
