package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TestProvisionerContext
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerRuntime
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.provisioner.pass.PassSecretRuntime
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabase
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseProvisioner
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseRuntime
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUser
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserProvisioner
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserRuntime
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerStatus
import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresProvisionersTest {

    val postgresContainer =
        GenericContainer("postgres:17").also {
            it.withEnv("POSTGRES_USER", "rds")
            it.withEnv("POSTGRES_PASSWORD", "verysecret")
            it.addExposedPort(5432)
            it.start()
        }

    @BeforeAll
    fun setup() {
        await().until { postgresContainer.execInContainer("pg_isready").exitCode == 0 }
    }

    @Test
    fun testFlow() {
        val serverProvisioner = mockk<HetznerServerProvisioner>()
        coEvery { serverProvisioner.lookup(any(), any()) } returns
            HetznerServerRuntime(
                1,
                "server1",
                ServerStatus.running,
                "debian12",
                HetznerServerType.cx23,
                HetznerLocation.nbg1,
                emptyMap(),
                emptyList(),
                null,
                "127.0.0.1",
                emptyList(),
            )
        coEvery { serverProvisioner.supportedLookupType } returns HetznerServerLookup::class

        val secretProvisioner = mockk<PassSecretProvisioner>()
        coEvery { secretProvisioner.lookup(match { it.name == "super_user_password" }, any()) } returns
            PassSecretRuntime(
                "super_user_password",
                "verysecret",
            )
        coEvery { secretProvisioner.lookup(match { it.name == "password1" }, any()) } returns
            PassSecretRuntime(
                "password1",
                "secret1",
            )
        coEvery { secretProvisioner.lookup(match { it.name == "password2" }, any()) } returns
            PassSecretRuntime(
                "password2",
                "secret2",
            )
        coEvery { secretProvisioner.supportedLookupType } returns PassSecretLookup::class

        val server =
            HetznerServer(
                "server1",
                HetznerLocation.nbg1,
                HetznerServerType.cx23,
                UserData(emptySet(), { "" }),
            )

        val userProvisioner = PostgresUserProvisioner()
        val databaseProvisioner = PostgresDatabaseProvisioner()

        val context =
            TestProvisionerContext(
                registry =
                ProvisionersRegistry(
                    listOf(
                        serverProvisioner,
                        secretProvisioner,
                        userProvisioner,
                        databaseProvisioner,
                    ),
                    listOf(
                        serverProvisioner,
                        secretProvisioner,
                        userProvisioner,
                        databaseProvisioner,
                    ),
                ),
                portMappings = mapOf(5432 to postgresContainer.getMappedPort(5432)),
            )

        val superUserPassword = PassSecret("super_user_password")
        val password1 = PassSecret("password1")
        val password2 = PassSecret("password2")

        runBlocking {
            val username = UUID.randomUUID().toString()
            val user =
                PostgresUser(
                    username,
                    password1.asLookup(),
                    server.asLookup(),
                    superUserPassword.asLookup(),
                )
            val userWithPassword2 =
                PostgresUser(
                    username,
                    password2.asLookup(),
                    server.asLookup(),
                    superUserPassword.asLookup(),
                )

            userProvisioner.lookup(user.asLookup(), context) shouldBe null
            assertSoftly(userProvisioner.diff(user, context)) {
                it.status shouldBe missing
                it.changes shouldHaveSize 0
            }

            userProvisioner
                .apply(user, context, TEST_LOG_CONTEXT)
                .shouldBeInstanceOf<Success<PostgresUserRuntime?>>()
                .data
                ?.name shouldBe username
            userProvisioner.lookup(user.asLookup(), context)?.name shouldBe username

            assertSoftly(userProvisioner.diff(userWithPassword2, context)) {
                it.status shouldBe has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "password"
                it.changes[0].changed shouldBe true
            }

            userProvisioner
                .apply(userWithPassword2, context, TEST_LOG_CONTEXT)
                .shouldBeInstanceOf<Success<PostgresUserRuntime?>>()
                .data
                ?.name shouldBe username
            assertSoftly(userProvisioner.diff(userWithPassword2, context)) {
                it.status shouldBe up_to_date
                it.changes shouldHaveSize 0
            }

            val database =
                PostgresDatabase(
                    UUID.randomUUID().toString(),
                    user.asLookup(),
                    server.asLookup(),
                    superUserPassword.asLookup(),
                )
            databaseProvisioner.lookup(database.asLookup(), context) shouldBe null
            assertSoftly(databaseProvisioner.diff(database, context)) {
                it.status shouldBe missing
                it.changes shouldHaveSize 0
            }

            assertSoftly(
                databaseProvisioner
                    .apply(database, context, TEST_LOG_CONTEXT)
                    .shouldBeInstanceOf<Success<PostgresDatabaseRuntime>>(),
            ) {
                it.data.name shouldBe database.name
            }

            databaseProvisioner.lookup(database.asLookup(), context)?.name shouldBe database.name
            assertSoftly(databaseProvisioner.diff(database, context)) {
                it.status shouldBe up_to_date
                it.changes shouldHaveSize 0
            }
        }
    }
}
