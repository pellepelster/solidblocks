package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.provisioner.context.ValidationContext
import de.solidblocks.cloud.services.postgres.PostgresSqlServiceManager
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationFactory
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseUserConfiguration
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import org.junit.jupiter.api.Test

class PostgresSqlServiceTest {

    private fun validate(databases: List<PostgresSqlServiceDatabaseConfiguration>) = PostgresSqlServiceManager().validateConfiguration(
        0,
        CloudConfiguration("name1", "blcks-test.de", emptyMap(), emptyList(), emptyList()),
        PostgresSqlServiceConfiguration(
            ServiceCommonConfig("service1", false, emptyMap(), InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23)),
            BackupConfig(16, 7),
            databases,
            17,
        ),
        mockk<ValidationContext>(),
        TEST_LOG_CONTEXT,
    )

    @Test
    fun `parse database users with permissions`() {
        val ymlRaw =
            """
        name: "name1"
        databases:
            - name: database1
              users:
                  - name: user1
                    admin: true
                  - name: user2
                    read: true
                    write: true
        """
                .trimIndent()

        val yaml = yamlParse(ymlRaw).shouldBeTypeOf<Success<YamlNode>>()
        val result = PostgresSqlServiceConfigurationFactory().parse(yaml.data)
        val configuration = result.shouldBeTypeOf<Success<PostgresSqlServiceConfiguration>>()
        configuration.data.name shouldBe "name1"
        configuration.data.databases shouldHaveSize 1

        val users = configuration.data.databases.first().users
        users shouldHaveSize 2

        users[0].name shouldBe "user1"
        users[0].admin shouldBe true
        users[0].read shouldBe false
        users[0].write shouldBe false

        users[1].name shouldBe "user2"
        users[1].admin shouldBe false
        users[1].read shouldBe true
        users[1].write shouldBe true
    }

    @Test
    fun `user name colliding with database name`() {
        val result = validate(
            listOf(
                PostgresSqlServiceDatabaseConfiguration("database1", emptyList()),
                PostgresSqlServiceDatabaseConfiguration(
                    "database2",
                    listOf(PostgresSqlServiceDatabaseUserConfiguration("database1", false, true, false)),
                ),
            ),
        ).shouldBeTypeOf<Error<PostgresSqlServiceConfigurationRuntime>>()

        result.error shouldBe
            "user with name 'database1' for database 'database2' collides with the default user for database 'database1'"
    }

    @Test
    fun `user configured for multiple databases`() {
        val result = validate(
            listOf(
                PostgresSqlServiceDatabaseConfiguration(
                    "database1",
                    listOf(PostgresSqlServiceDatabaseUserConfiguration("user1", false, true, false)),
                ),
                PostgresSqlServiceDatabaseConfiguration(
                    "database2",
                    listOf(PostgresSqlServiceDatabaseUserConfiguration("user1", false, true, false)),
                ),
            ),
        ).shouldBeTypeOf<Error<PostgresSqlServiceConfigurationRuntime>>()

        result.error shouldBe
            "user with name 'user1' is configured for multiple databases, sharing users between databases is not supported"
    }

    @Test
    fun `user name rds is reserved`() {
        val result = validate(
            listOf(
                PostgresSqlServiceDatabaseConfiguration(
                    "database1",
                    listOf(PostgresSqlServiceDatabaseUserConfiguration("rds", false, true, false)),
                ),
            ),
        ).shouldBeTypeOf<Error<PostgresSqlServiceConfigurationRuntime>>()

        result.error shouldBe "user name 'rds' for database 'database1' is reserved for the superuser"
    }
}
