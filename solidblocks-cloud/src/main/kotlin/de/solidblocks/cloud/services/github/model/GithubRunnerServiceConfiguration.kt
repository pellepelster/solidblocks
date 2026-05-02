package de.solidblocks.cloud.services.github.model

import de.solidblocks.cloud.providers.CloudResourceProviderConfiguration
import de.solidblocks.cloud.providers.github.GithubProviderConfiguration
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.ServiceConfiguration
import kotlin.reflect.KClass

data class GithubRunnerServiceConfiguration(
    override val name: String,
    val instance: InstanceConfig,
    val labels: List<String>,
    val packages: List<String>,
    val allowSudo: Boolean,
    val scale: Int,
) : ServiceConfiguration {
    override val type = "github_runner"
    override val neededProviders: List<KClass<*>> = listOf(GithubProviderConfiguration::class, CloudResourceProviderConfiguration::class)
}
