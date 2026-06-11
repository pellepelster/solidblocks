package de.solidblocks.cloud.services.docker.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.StringListKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.BackupConfigurationFactory
import de.solidblocks.cloud.services.ServiceConfigurationFactory
import de.solidblocks.cloud.services.ServiceConfigurationFactory.parseServiceCommonConfig
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result

class DockerServiceConfigurationFactory : PolymorphicConfigurationFactory<DockerServiceConfiguration>() {

    val image =
        StringKeyword(
            "image",
            KeywordHelp(
                "Docker image to deploy",
            ),
        )

    val endpoints =
        ListKeyword(
            "endpoints",
            DockerServiceEndpointConfigurationFactory(),
            KeywordHelp("Service endpoints to publicly expose"),
        )

    val links =
        StringListKeyword(
            "links",
            KeywordHelp(
                "Linked services will automatically expose environment variables to the linked service, e.g. database credentials. To see which variables are available run the `info` command.",
            ),
        )

    override val help =
        ConfigurationHelp(
            "Docker",
            "Deploys a dockerized service exposes its endpoints.",
        )

    override val keywords =
        listOf(endpoints, image, links) +
            BackupConfigurationFactory.keywords +
            ServiceConfigurationFactory.keywords

    override fun parse(yaml: YamlNode): Result<DockerServiceConfiguration> = result {
        DockerServiceConfiguration(
            yaml.parseServiceCommonConfig().bind(),
            image.parse(yaml).bind(),
            BackupConfigurationFactory.parse(yaml).bind(),
            endpoints.parse(yaml).bind(),
            links.parse(yaml).bind(),
        )
    }
}
