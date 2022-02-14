package de.solidblocks.cloud.model

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cloud.model.repositories.UsersRepository
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestEnvironmentExtension::class)
class UsersRepositoryTest {

    @Test
    fun testCreateEnvironmentUser(testEnvironment: TestEnvironment) {
        val cloud = UUID.randomUUID().toString()
        testEnvironment.testContext.createCloudEnvironment(cloud, "env1")

        val repository = UsersRepository(testEnvironment.database.dsl, testEnvironment.repositories.clouds, testEnvironment.repositories.environments, testEnvironment.repositories.tenants)

        assertThat(repository.getUser("juergen@$cloud")).isNull()
        assertThat(repository.createEnvironmentUser(EnvironmentReference(cloud, "env1"), "juergen@$cloud", "password2", "salt2")).isNotNull

        val user = repository.getUser("juergen@$cloud")
        assertThat(user).isNotNull
        assertThat(user?.email).isEqualTo("juergen@$cloud")
        assertThat(user?.password).isEqualTo("password2")
        assertThat(user?.salt).isEqualTo("salt2")
    }

    @Test
    fun testCreateTenantUser(testEnvironment: TestEnvironment) {
        val cloud = UUID.randomUUID().toString()
        testEnvironment.testContext.createCloudEnvironmentTenant(cloud, "env1", "tenant1")

        val repository = UsersRepository(testEnvironment.database.dsl, testEnvironment.repositories.clouds, testEnvironment.repositories.environments, testEnvironment.repositories.tenants)

        assertThat(repository.getUser("juergen@$cloud")).isNull()
        assertThat(repository.createTenantUser(TenantReference(cloud, "env1", "tenant1"), "juergen@$cloud", "password2", "salt2")).isNotNull

        val user = repository.getUser("juergen@$cloud")
        assertThat(user).isNotNull
        assertThat(user?.email).isEqualTo("juergen@$cloud")
        assertThat(user?.password).isEqualTo("password2")
        assertThat(user?.salt).isEqualTo("salt2")
    }
}
