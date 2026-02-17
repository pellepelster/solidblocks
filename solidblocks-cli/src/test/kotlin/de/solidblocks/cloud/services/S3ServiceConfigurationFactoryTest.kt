package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationFactory
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class S3ServiceConfigurationFactoryTest {

  @Test
  fun testParse() {
    val ymlRaw =
        """
        name: "name1"
        buckets:
            - name: "bucket1"
        """
            .trimIndent()

    val yaml = yamlParse(ymlRaw).shouldBeTypeOf<Success<YamlNode>>()
    val result = S3ServiceConfigurationFactory().parse(yaml.data)
    val configuration = result.shouldBeTypeOf<Success<S3ServiceConfiguration>>()
    configuration.data.name shouldBe "name1"
    configuration.data.buckets shouldHaveSize 1
  }
}
