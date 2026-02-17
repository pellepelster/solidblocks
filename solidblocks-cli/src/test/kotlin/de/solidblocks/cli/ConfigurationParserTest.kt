package de.solidblocks.cli

import de.solidblocks.cli.mock.TestConfiguration
import de.solidblocks.cli.mock.TestConfigurationFactory
import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class ConfigurationParserTest {

  @Test
  fun testParseConfiguration() {
    val rawYml =
        """
        test1:
          - name: foo
            flag1: true
          - name: bar
            flag1: false
        polymorphic_list1:
          - name: foo
            type: list_type1
          - name: bar
            type: list_type2
        test2:
          number1: 13
        """
            .trimIndent()

    val result = ConfigurationParser(TestConfigurationFactory()).parse(rawYml)
    val testConfiguration = result.shouldBeTypeOf<Success<TestConfiguration>>()

    testConfiguration.data.test1 shouldHaveSize 2
    testConfiguration.data.test1[0].name shouldBe "foo"
    testConfiguration.data.test1[0].flag1 shouldBe true
    testConfiguration.data.test1[1].name shouldBe "bar"
    testConfiguration.data.test1[1].flag1 shouldBe false

    testConfiguration.data.test2.number1 shouldBe 13
  }

  @Test
  fun testParseConfigurationError() {
    val rawYml =
        """
        test1:
          - name: foo
            flag1: true
          - name: forbidden
            flag1: false
        """
            .trimIndent()

    val result = ConfigurationParser(TestConfigurationFactory()).parse(rawYml)
    val testConfiguration = result.shouldBeTypeOf<Error<TestConfiguration>>()
    testConfiguration.error shouldBe "name 'forbidden' is not allowed at line 4 colum 5"
  }
}
