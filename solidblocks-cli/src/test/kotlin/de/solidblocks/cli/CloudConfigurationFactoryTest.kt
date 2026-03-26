package de.solidblocks.cli

import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class CloudConfigurationFactoryTest {

  @Test
  fun testParseCloud() {
    val rawYml =
        """
        name: foo-bar
        root_domain: foo.bar
        """
            .trimIndent()

    val cloud =
        ConfigurationParser(CloudConfigurationFactory(emptyList(), emptyList()))
            .parse(rawYml)
            .shouldBeTypeOf<Success<CloudConfiguration>>()

    cloud.data.name shouldBe "foo-bar"
    cloud.data.services.shouldBeEmpty()
    cloud.data.providers.shouldBeEmpty()
  }

  @Test
  fun testParseUnknownService() {
    val rawYml =
        """
        name: foo-bar
        root_domain: foo.bar
        services:
            - name: foo-bar
              type: invalid

        """
            .trimIndent()

    val cloud =
        ConfigurationParser(CloudConfigurationFactory(emptyList(), emptyList()))
            .parse(rawYml)
            .shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
    cloud.error shouldBe "unknown type 'invalid', possible types are <none> at line 4 colum 7"
  }
}
