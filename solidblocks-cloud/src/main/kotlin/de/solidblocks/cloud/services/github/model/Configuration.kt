package de.solidblocks.cloud.services.github.model

import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class GithubRunnerServiceConfiguration(
    override val name: String,
    val labels: String,
    val instance: InstanceConfig,
) : ServiceConfiguration {
    override val type = "github_runner"
}
