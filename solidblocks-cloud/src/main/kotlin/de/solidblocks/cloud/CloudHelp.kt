package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.documentation.DocumentationGenerator

class CloudHelp : BaseCloudManager() {
    fun renderMarkdown(hugo: Boolean): String {
        return DocumentationGenerator(hugo).generateMarkdown(CloudConfigurationFactory(providerRegistrations, serviceRegistrations))
    }
}