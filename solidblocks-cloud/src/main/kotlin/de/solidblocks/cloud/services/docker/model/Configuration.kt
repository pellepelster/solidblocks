package de.solidblocks.cloud.services.docker.model

import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class DockerServiceConfiguration(
    override val name: String,
    val image: String,
    val instance: InstanceConfig,
    val backup: BackupConfig,
    val endpoints: List<DockerServiceEndpointConfiguration>,
    val links: List<String>,
) : ServiceConfiguration {
  override val type = "docker"
}

data class DockerServiceEndpointConfiguration(val port: Int)
