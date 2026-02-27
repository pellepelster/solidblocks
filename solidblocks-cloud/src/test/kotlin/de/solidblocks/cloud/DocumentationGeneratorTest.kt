package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.documentation.DocumentationGenerator
import org.junit.jupiter.api.Test

class DocumentationGeneratorTest {
    @Test
    fun `generate documentation`() {
        println(DocumentationGenerator().generateMarkdown(CloudConfigurationFactory(emptyList(), emptyList())))
    }
}