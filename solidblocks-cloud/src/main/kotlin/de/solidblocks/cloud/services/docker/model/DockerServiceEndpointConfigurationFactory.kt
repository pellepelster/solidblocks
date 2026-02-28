package de.solidblocks.cloud.services.docker.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.IP_PORTS
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import io.github.oshai.kotlinlogging.KotlinLogging

class DockerServiceEndpointConfigurationFactory : ConfigurationFactory<DockerServiceEndpointConfiguration> {

    private val logger = KotlinLogging.logger {}

    val port =
        NumberKeyword(
            "port",
            IP_PORTS,
            KeywordHelp(
                "Service port on the docker container",
            ),
        )

    val endpointTypes = StringConstraints(
        options = listOf(
            "http",
        )
    )

    val type =
        StringKeywordOptionalWithDefault(
            "type",
            endpointTypes,
            endpointTypes.options.first(),
            KeywordHelp(
                "Type of the service endpoints. Endpoints with the type `http` are automatically terminated with TLS if a `root_domain` is set.",
            )
        )


    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf(port, type)

    override fun parse(yaml: YamlNode): Result<DockerServiceEndpointConfiguration> {
        val port =
            when (val result = port.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<Int> -> result.data
            }

        return Success(DockerServiceEndpointConfiguration(port))
    }
}
