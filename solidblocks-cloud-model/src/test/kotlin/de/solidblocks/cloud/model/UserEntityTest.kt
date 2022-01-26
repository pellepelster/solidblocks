package de.solidblocks.cloud.model

import de.solidblocks.cloud.model.entities.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class UserEntityTest {

    @Test
    fun testPermissions() {
        val adminUser = UserEntity(UUID.randomUUID(), "email", "salt", "password", true).permissions()
        assertThat(adminUser.isCloudWildcard).isTrue
        assertThat(adminUser.clouds).isEmpty()
        assertThat(adminUser.isEnvironmentWildcard).isTrue
        assertThat(adminUser.environments).isEmpty()
        assertThat(adminUser.isTenantWildcard).isTrue
        assertThat(adminUser.tenants).isEmpty()

        val cloud = mockk<CloudEntity>()
        every { cloud.name } returns "cloud1"

        val cloudUser = UserEntity(UUID.randomUUID(), "email", "salt", "password", false, cloud = cloud).permissions()
        assertThat(cloudUser.isCloudWildcard).isFalse
        assertThat(cloudUser.clouds).isEqualTo(listOf("cloud1"))
        assertThat(cloudUser.isEnvironmentWildcard).isTrue
        assertThat(cloudUser.environments).isEmpty()
        assertThat(cloudUser.isTenantWildcard).isTrue
        assertThat(cloudUser.tenants).isEmpty()

        val environment = mockk<EnvironmentEntity>()
        every { environment.name } returns "environment1"
        every { environment.cloud } returns cloud

        val environmentUser =
            UserEntity(UUID.randomUUID(), "email", "salt", "password", false, environment = environment).permissions()
        assertThat(environmentUser.isCloudWildcard).isFalse
        assertThat(environmentUser.clouds).isEqualTo(listOf("cloud1"))
        assertThat(environmentUser.isEnvironmentWildcard).isFalse
        assertThat(environmentUser.environments).isEqualTo(listOf("environment1"))
        assertThat(environmentUser.isTenantWildcard).isTrue
        assertThat(environmentUser.tenants).isEmpty()

        val tenant = mockk<TenantEntity>()
        every { tenant.name } returns "tenant1"
        every { tenant.environment } returns environment

        val tenantUser =
            UserEntity(UUID.randomUUID(), "email", "salt", "password", false, tenant = tenant).permissions()
        assertThat(tenantUser.isCloudWildcard).isFalse
        assertThat(tenantUser.clouds).isEqualTo(listOf("cloud1"))
        assertThat(tenantUser.isEnvironmentWildcard).isFalse
        assertThat(tenantUser.environments).isEqualTo(listOf("environment1"))
        assertThat(tenantUser.isTenantWildcard).isFalse
        assertThat(tenantUser.tenants).isEqualTo(listOf("tenant1"))
    }

    @Test
    fun testScopes() {

        assertThat(
            UserEntity(
                UUID.randomUUID(),
                "email",
                "salt",
                "password",
                false,
                cloud = mockk()
            ).scope()
        ).isEqualTo(Scope.cloud)
        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", true, cloud = mockk()).scope()).isEqualTo(
            Scope.cloud
        )

        assertThat(
            UserEntity(
                UUID.randomUUID(),
                "email",
                "salt",
                "password",
                false,
                environment = mockk()
            ).scope()
        ).isEqualTo(Scope.environment)
        assertThat(
            UserEntity(
                UUID.randomUUID(),
                "email",
                "salt",
                "password",
                true,
                environment = mockk()
            ).scope()
        ).isEqualTo(Scope.environment)

        assertThat(
            UserEntity(
                UUID.randomUUID(),
                "email",
                "salt",
                "password",
                false,
                tenant = mockk()
            ).scope()
        ).isEqualTo(Scope.tenant)
        assertThat(
            UserEntity(
                UUID.randomUUID(),
                "email",
                "salt",
                "password",
                true,
                tenant = mockk()
            ).scope()
        ).isEqualTo(Scope.tenant)

        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", true).scope()).isEqualTo(Scope.root)
    }

    @Test
    fun testInvalidScopes() {
        assertThrows(RuntimeException::class.java) {
            assertThat(
                UserEntity(
                    UUID.randomUUID(),
                    "email",
                    "salt",
                    "password",
                    false
                ).scope()
            ).isEqualTo(Scope.root)
        }

        assertThrows(RuntimeException::class.java) {
            assertThat(
                UserEntity(
                    UUID.randomUUID(),
                    "email",
                    "salt",
                    "password",
                    cloud = mockk(),
                    environment = mockk(),
                    tenant = mockk()
                ).scope()
            ).isEqualTo(Scope.root)
        }

        assertThrows(RuntimeException::class.java) {
            assertThat(
                UserEntity(
                    UUID.randomUUID(),
                    "email",
                    "salt",
                    "password",
                    cloud = mockk(),
                    environment = mockk()
                ).scope()
            ).isEqualTo(Scope.root)
        }
        assertThrows(RuntimeException::class.java) {
            assertThat(
                UserEntity(
                    UUID.randomUUID(),
                    "email",
                    "salt",
                    "password",
                    cloud = mockk(),
                    tenant = mockk()
                ).scope()
            ).isEqualTo(Scope.root)
        }
        assertThrows(RuntimeException::class.java) {
            assertThat(
                UserEntity(
                    UUID.randomUUID(),
                    "email",
                    "salt",
                    "password",
                    environment = mockk(),
                    tenant = mockk()
                ).scope()
            ).isEqualTo(Scope.root)
        }
    }
}
