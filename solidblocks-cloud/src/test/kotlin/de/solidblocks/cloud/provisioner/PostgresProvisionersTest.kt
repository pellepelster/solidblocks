package de.solidblocks.cloud.provisioner
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TestProvisionerContext
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.diffData
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerRuntime
import de.solidblocks.cloud.provisioner.pass.*
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabase
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseProvisioner
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseRuntime
import de.solidblocks.cloud.provisioner.postgres.grant.PostgresDatabaseGrant
import de.solidblocks.cloud.provisioner.postgres.grant.PostgresDatabaseGrantProvisioner
import de.solidblocks.cloud.provisioner.postgres.grant.PostgresDatabaseGrantRuntime
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUser
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserProvisioner
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserRuntime
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.RandomSecret
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.UserDataResult
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerStatus
import de.solidblocks.ssh.SSHClient
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import io.mockk.coEvery
import io.mockk.mockk
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.postgresql.util.PSQLException
import org.testcontainers.containers.GenericContainer
import java.sql.Connection
import java.sql.DriverManager
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
            GenericSecretRuntime(
                "super_user_password",
                "verysecret",
            )
        coEvery { secretProvisioner.lookup(match { it.name == "password1" }, any()) } returns
            GenericSecretRuntime(
                "password1",
                "secret1",
            )
        coEvery { secretProvisioner.lookup(match { it.name == "password2" }, any()) } returns
            GenericSecretRuntime(
                "password2",
                "secret2",
            )
        coEvery { secretProvisioner.supportedLookupType } returns GenericSecretLookup::class

        val server =
            HetznerServer(
                "server1",
                HetznerLocation.nbg1,
                HetznerServerType.cx23,
                UserData(emptySet(), { UserDataResult("", "") }),
            )

        val userProvisioner = PostgresUserProvisioner()
        val databaseProvisioner = PostgresDatabaseProvisioner()
        val grantProvisioner = PostgresDatabaseGrantProvisioner()

        val sshClient = mockk<SSHClient>()
        coEvery {
            sshClient.portForward<Any>(remotePort = 5432, localPort = any(), block = captureLambda())
        } coAnswers {
            lambda<suspend (Int?) -> Any>().coInvoke(postgresContainer.getMappedPort(5432))
        }

        val context =
            TestProvisionerContext(
                registry =
                ProvisionersRegistry(
                    listOf(
                        serverProvisioner,
                        secretProvisioner,
                        userProvisioner,
                        databaseProvisioner,
                        grantProvisioner,
                    ),
                    listOf(
                        serverProvisioner,
                        secretProvisioner,
                        userProvisioner,
                        databaseProvisioner,
                        grantProvisioner,
                    ),
                ),
                sshClient,
            )

        val superUserPassword = GenericSecret<GenericSecretRuntime>("super_user_password", RandomSecret(), true)
        val password1 = GenericSecret<GenericSecretRuntime>("password1", RandomSecret(), true)
        val password2 = GenericSecret<GenericSecretRuntime>("password2", RandomSecret(), true)

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
            assertSoftly(userProvisioner.diff(user, context).diffData()) {
                it.status shouldBe missing
                it.changes shouldHaveSize 0
            }

            userProvisioner
                .apply(user, context, TEST_LOG_CONTEXT)
                .shouldBeInstanceOf<Success<PostgresUserRuntime?>>()
                .data
                ?.name shouldBe username
            userProvisioner.lookup(user.asLookup(), context)?.name shouldBe username

            assertSoftly(userProvisioner.diff(userWithPassword2, context).diffData()) {
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
            assertSoftly(userProvisioner.diff(userWithPassword2, context).diffData()) {
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
            assertSoftly(databaseProvisioner.diff(database, context).diffData()) {
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
            assertSoftly(databaseProvisioner.diff(database, context).diffData()) {
                it.status shouldBe up_to_date
                it.changes shouldHaveSize 0
            }

            val grantUsername = UUID.randomUUID().toString()
            val grantUser =
                PostgresUser(
                    grantUsername,
                    password1.asLookup(),
                    server.asLookup(),
                    superUserPassword.asLookup(),
                )
            userProvisioner
                .apply(grantUser, context, TEST_LOG_CONTEXT)
                .shouldBeInstanceOf<Success<PostgresUserRuntime?>>()

            val readGrant = PostgresDatabaseGrant(grantUser, database, false, true, false, server.asLookup(), superUserPassword.asLookup())

            grantProvisioner.lookup(readGrant.asLookup(), context) shouldBe null
            assertSoftly(grantProvisioner.diff(readGrant, context).diffData()) {
                it.status shouldBe missing
                it.changes shouldHaveSize 0
            }

            assertSoftly(
                grantProvisioner
                    .apply(readGrant, context, TEST_LOG_CONTEXT)
                    .shouldBeInstanceOf<Success<PostgresDatabaseGrantRuntime>>(),
            ) {
                it.data.admin shouldBe false
                it.data.read shouldBe true
                it.data.write shouldBe false
            }
            assertSoftly(grantProvisioner.diff(readGrant, context).diffData()) {
                it.status shouldBe up_to_date
                it.changes shouldHaveSize 0
            }

            // tables created by the database owner after the grant are covered via default privileges
            connect(database.name, username, "secret2").use {
                it.createStatement().use { statement ->
                    statement.execute("CREATE TABLE table1 (id INT)")
                    statement.execute("INSERT INTO table1 VALUES (1)")
                }
            }

            connect(database.name, grantUsername, "secret1").use {
                it.createStatement().use { statement ->
                    statement.executeQuery("SELECT * FROM table1").use { rs -> rs.next() shouldBe true }
                    shouldThrow<PSQLException> { statement.execute("INSERT INTO table1 VALUES (2)") }
                }
            }

            assertSoftly(grantProvisioner.diff(readGrant, context).diffData()) {
                it.status shouldBe up_to_date
                it.changes shouldHaveSize 0
            }

            val writeGrant = PostgresDatabaseGrant(grantUser, database, false, false, true, server.asLookup(), superUserPassword.asLookup())
            assertSoftly(grantProvisioner.diff(writeGrant, context).diffData()) {
                it.status shouldBe has_changes
                it.changes shouldHaveSize 2
            }

            grantProvisioner
                .apply(writeGrant, context, TEST_LOG_CONTEXT)
                .shouldBeInstanceOf<Success<PostgresDatabaseGrantRuntime>>()
            assertSoftly(grantProvisioner.diff(writeGrant, context).diffData()) {
                it.status shouldBe up_to_date
                it.changes shouldHaveSize 0
            }

            connect(database.name, grantUsername, "secret1").use {
                it.createStatement().use { statement ->
                    statement.execute("INSERT INTO table1 VALUES (2)")
                    shouldThrow<PSQLException> { statement.executeQuery("SELECT * FROM table1") }
                }
            }

            val adminGrant = PostgresDatabaseGrant(grantUser, database, true, false, false, server.asLookup(), superUserPassword.asLookup())
            assertSoftly(grantProvisioner.diff(adminGrant, context).diffData()) {
                it.status shouldBe has_changes
            }
            assertSoftly(
                grantProvisioner
                    .apply(adminGrant, context, TEST_LOG_CONTEXT)
                    .shouldBeInstanceOf<Success<PostgresDatabaseGrantRuntime>>(),
            ) {
                it.data.admin shouldBe true
                it.data.read shouldBe true
                it.data.write shouldBe true
            }
            assertSoftly(grantProvisioner.diff(adminGrant, context).diffData()) {
                it.status shouldBe up_to_date
                it.changes shouldHaveSize 0
            }

            connect(database.name, grantUsername, "secret1").use {
                it.createStatement().use { statement ->
                    statement.execute("CREATE TABLE table2 (id INT)")
                    statement.executeQuery("SELECT * FROM table1").use { rs -> rs.next() shouldBe true }
                }
            }
        }
    }

    fun connect(database: String, user: String, password: String): Connection = DriverManager.getConnection(
        "jdbc:postgresql://localhost:${postgresContainer.getMappedPort(5432)}/$database",
        user,
        password,
    )
}
