package de.solidblocks.cloud.services.docker.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
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
          KeywordHelp("Service endpoints to expose"),
      )

  val links =
      StringListKeyword(
          "links",
          KeywordHelp(
              "Linked services will automatically expose connection information to the linked service as environment variables, e.g. database credentials",
          ),
      )

  override val help =
      ConfigurationHelp(
          "Docker",
          "Deploys a docker service image containers and exposes its endpoints",
      )

  override val keywords = listOf(SERVICE_NAME_KEYWORD, endpoints, image, links)

  override fun parse(yaml: YamlNode): Result<DockerServiceConfiguration> {
    val name =
        when (val name = SERVICE_NAME_KEYWORD.parse(yaml)) {
          is Error<*> -> return Error(name.error)
          is Success<String> -> name.data
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

    return Success(DockerServiceConfiguration(name, endpoints, links))
  }
}
