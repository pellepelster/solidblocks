package de.solidblocks.cloud.model

import de.solidblocks.cloud.model.entities.Scope
import de.solidblocks.cloud.model.entities.UserEntity
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class UserEntityTest {

    @Test
    fun testPermissions() {
    }

    @Test
    fun testScope() {

        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", false, cloud = mockk()).scope()).isEqualTo(Scope.cloud)
        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", true, cloud = mockk()).scope()).isEqualTo(Scope.cloud)

        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", false, environment = mockk()).scope()).isEqualTo(Scope.environment)
        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", true, environment = mockk()).scope()).isEqualTo(Scope.environment)

        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", false, tenant = mockk()).scope()).isEqualTo(Scope.tenant)
        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", true, tenant = mockk()).scope()).isEqualTo(Scope.tenant)

        assertThrows(RuntimeException::class.java) { assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", false).scope()).isEqualTo(Scope.root) }
        assertThat(UserEntity(UUID.randomUUID(), "email", "salt", "password", true).scope()).isEqualTo(Scope.root)
    }
}
