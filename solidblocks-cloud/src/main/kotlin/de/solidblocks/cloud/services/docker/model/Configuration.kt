package de.solidblocks.cloud.services.docker.model

import de.solidblocks.cloud.providers.CloudResourceProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.github.GithubProviderConfiguration
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfiguration
import de.solidblocks.cloud.providers.types.secret.SecretProviderConfiguration
import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.ServiceConfiguration
import kotlin.reflect.KClass

data class DockerServiceConfiguration(
    override val name: String,
    val image: String,
    val instance: InstanceConfig,
    val backup: BackupConfig,
    val endpoints: List<DockerServiceEndpointConfiguration>,
    val links: List<String>,
) : ServiceConfiguration {
    override val type = "docker"
    override val neededProviders: List<KClass<*>> = listOf(BackupProviderConfiguration::class, SecretProviderConfiguration::class, CloudResourceProviderConfiguration::class)
}

data class DockerServiceEndpointConfiguration(val port: Int)
