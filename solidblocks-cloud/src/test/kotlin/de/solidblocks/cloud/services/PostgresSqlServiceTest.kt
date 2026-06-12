package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationFactory
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class PostgresSqlServiceTest {

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
}
