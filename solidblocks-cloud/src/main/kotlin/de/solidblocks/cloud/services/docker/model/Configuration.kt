package de.solidblocks.cloud.services.docker.model

import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.ServiceCommonConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class DockerServiceConfiguration(
    override val common: ServiceCommonConfig,
    val image: String,
    val backup: BackupConfig,
    val endpoints: List<DockerServiceEndpointConfiguration>,
    val links: List<String>,
) : ServiceConfiguration {
    override val type = "docker"
}

data class DockerServiceEndpointConfiguration(val port: Int)
