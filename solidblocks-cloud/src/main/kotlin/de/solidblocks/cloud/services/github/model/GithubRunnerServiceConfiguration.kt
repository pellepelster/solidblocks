package de.solidblocks.cloud.services.github.model

import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class GithubRunnerServiceConfiguration(
    override val name: String,
    val instance: InstanceConfig,
    val labels: List<String>,
    val packages: List<String>,
    val allowSudo: Boolean,
) : ServiceConfiguration {
    override val type = "github_runner"
}
