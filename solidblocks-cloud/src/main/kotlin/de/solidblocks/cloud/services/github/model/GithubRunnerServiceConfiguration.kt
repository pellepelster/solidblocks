package de.solidblocks.cloud.services.github.model

import de.solidblocks.cloud.services.ServiceCommonConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class GithubRunnerServiceConfiguration(
    override val common: ServiceCommonConfig,
    val labels: List<String>,
    val packages: List<String>,
    val allowSudo: Boolean,
    val scale: Int,
) : ServiceConfiguration {
    override val type = "github_runner"
}
