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
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.InstanceConfigurationFactory
import de.solidblocks.cloud.services.SERVICE_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class DockerServiceConfigurationFactory :
    PolymorphicConfigurationFactory<DockerServiceConfiguration>() {

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
          "Deploys a docker service image containers and exposes its endpoints",
      )

  override val keywords =
      listOf(SERVICE_NAME_KEYWORD, endpoints, image, links) +
          BackupConfigurationFactory.keywords +
          InstanceConfigurationFactory.keywords

  override fun parse(yaml: YamlNode): Result<DockerServiceConfiguration> {
    val name =
        when (val result = SERVICE_NAME_KEYWORD.parse(yaml)) {
          is Error<*> -> return Error(result.error)
          is Success<String> -> result.data
        }

    val image =
        when (val result = image.parse(yaml)) {
          is Error<*> -> return Error(result.error)
          is Success<String> -> result.data
        }

    val instance =
        when (val result = InstanceConfigurationFactory.parse(yaml)) {
          is Error<InstanceConfig> -> return Error(result.error)
          is Success<InstanceConfig> -> result.data
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
        DockerServiceConfiguration(name, image, instance, backupConfig, endpoints, links),
    )
  }
}
