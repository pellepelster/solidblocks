package de.solidblocks.cli

import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class CloudFactoryTest {

  @Test
  fun testParseCloud() {
    val rawYml =
        """
        name: foo-bar
        root_domain: foo.bar
        """
            .trimIndent()

    val result =
        ConfigurationParser(CloudConfigurationFactory(emptyList(), emptyList())).parse(rawYml)
    val cloud = result.shouldBeTypeOf<Success<CloudConfigurationRuntime>>()

    cloud.data.name shouldBe "foo-bar"
  }
}
