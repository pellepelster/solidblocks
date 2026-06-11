package de.solidblocks.cloud.services.docker.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.configuration.NumberConstraints.Companion.IP_PORTS
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result
import io.github.oshai.kotlinlogging.KotlinLogging

class DockerServiceEndpointConfigurationFactory : ConfigurationFactory<DockerServiceEndpointConfiguration> {

    private val logger = KotlinLogging.logger {}

    val port =
        NumberKeyword(
            "container_port",
            KeywordHelp(
                "Service port on the docker container",
            ),
        ).constraints(IP_PORTS)

    val endpointTypes =
        StringConstraints(
            options =
            listOf(
                "http",
            ),
        )

    val type =
        StringKeyword(
            "type",
            KeywordHelp(
                "Type of the service endpoints. Endpoints with the type `http` are automatically terminated with TLS if a `root_domain` is set.",
            ),
        ).constraints(endpointTypes).default(endpointTypes.options.first())

    override val help: ConfigurationHelp
        get() = TODO("Not yet implemented")

    override val keywords = listOf(port, type)

    override fun parse(yaml: YamlNode): Result<DockerServiceEndpointConfiguration> = result {
        DockerServiceEndpointConfiguration(port.parse(yaml).bind())
    }
}
