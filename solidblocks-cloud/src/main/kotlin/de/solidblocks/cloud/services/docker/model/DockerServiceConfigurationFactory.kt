package de.solidblocks.cloud.services.docker.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.ListKeyword
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.SERVICE_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
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
        ListKeyword("endpoints", DockerServiceEndpointConfigurationFactory(), KeywordHelp("Service endpoints to expose"))

    override val help = ConfigurationHelp("Docker", "Deploys a docker service image containers and exposes its endpoints")

    override val keywords = listOf(SERVICE_NAME_KEYWORD, endpoints, image)

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

        return Success(DockerServiceConfiguration(name, endpoints))
    }
}
