package de.solidblocks.cloud.services.docker.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.StringListKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.BackupConfigurationFactory
import de.solidblocks.cloud.services.ServiceCommonConfig
import de.solidblocks.cloud.services.ServiceConfigurationFactory
import de.solidblocks.cloud.services.ServiceConfigurationFactory.parseServiceCommonConfig
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class DockerServiceConfigurationFactory : PolymorphicConfigurationFactory<DockerServiceConfiguration>() {

    val image =
        StringKeyword(
            "image",
            NONE,
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

    override fun parse(yaml: YamlNode): Result<DockerServiceConfiguration> {
        val common = when (val result = yaml.parseServiceCommonConfig()) {
            is Error<ServiceCommonConfig> -> return Error(result.error)
            is Success<ServiceCommonConfig> -> result.data
        }

        val image =
            when (val result = image.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val backupConfig =
            when (val result = BackupConfigurationFactory.parse(yaml)) {
                is Error<BackupConfig> -> return Error(result.error)
                is Success<BackupConfig> -> result.data
            }

        val endpoints =
            when (val result = endpoints.parse(yaml)) {
                is Error<List<DockerServiceEndpointConfiguration>> -> return Error(result.error)
                is Success<List<DockerServiceEndpointConfiguration>> -> result.data
            }

        val links =
            when (val result = links.parse(yaml)) {
                is Error<List<String>> -> return Error(result.error)
                is Success<List<String>> -> result.data
            }

        return Success(
            DockerServiceConfiguration(common, image, backupConfig, endpoints, links),
        )
    }
}
