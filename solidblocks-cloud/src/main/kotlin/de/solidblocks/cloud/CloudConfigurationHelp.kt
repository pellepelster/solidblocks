package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.documentation.DocumentationGenerator
import de.solidblocks.cloud.documentation.JsonSchemaGenerator

class CloudConfigurationHelp : BaseCloudManager() {
    fun renderMarkdown(hugo: Boolean): String = DocumentationGenerator(hugo)
        .generateMarkdown(CloudConfigurationFactory(providerRegistrations, serviceRegistrations))

    fun renderJsonSchema(): String = JsonSchemaGenerator()
        .generateJsonSchema(CloudConfigurationFactory(providerRegistrations, serviceRegistrations))
}
