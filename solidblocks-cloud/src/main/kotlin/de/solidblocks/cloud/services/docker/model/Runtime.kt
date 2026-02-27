package de.solidblocks.cloud.services.docker.model

import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class DockerServiceEndpointConfigurationRuntime(val port: Int)

data class DockerServiceConfigurationRuntime(
    override val name: String,
    val buckets: List<DockerServiceEndpointConfigurationRuntime>,
) : ServiceConfigurationRuntime
