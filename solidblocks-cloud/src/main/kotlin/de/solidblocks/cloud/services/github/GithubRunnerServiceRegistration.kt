package de.solidblocks.cloud.services.github

import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfiguration
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfigurationFactory
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfigurationRuntime

class GithubRunnerServiceRegistration : ServiceRegistration<GithubRunnerServiceConfiguration, GithubRunnerServiceConfigurationRuntime> {

    override val type = "github_runner"

    override val supportedConfiguration = GithubRunnerServiceConfiguration::class
    override val supportedRuntime = GithubRunnerServiceConfigurationRuntime::class

    override fun createManager() = GithubRunnerServiceManager()

    override fun createFactory() = GithubRunnerServiceConfigurationFactory()
}
