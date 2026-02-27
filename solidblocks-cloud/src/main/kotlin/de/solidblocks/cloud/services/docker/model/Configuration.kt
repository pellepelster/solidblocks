package de.solidblocks.cloud.services.docker.model

import de.solidblocks.cloud.services.ServiceConfiguration

data class DockerServiceConfiguration(
    override val name: String,
    val endpoints: List<DockerServiceEndpointConfiguration>,
) : ServiceConfiguration {
    override val type = "docker"
}

data class DockerServiceEndpointConfiguration(val port: Int)
