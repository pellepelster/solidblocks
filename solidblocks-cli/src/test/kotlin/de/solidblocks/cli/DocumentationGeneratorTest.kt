package de.solidblocks.cli

import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.documentation.DocumentationGenerator
import org.junit.jupiter.api.Test

class DocumentationGeneratorTest {
  @Test
  fun `generate documentation`() {
    val generator = DocumentationGenerator(CloudConfigurationFactory(emptyList(), emptyList()))

    println(generator.generateMarkdown())
  }
}
