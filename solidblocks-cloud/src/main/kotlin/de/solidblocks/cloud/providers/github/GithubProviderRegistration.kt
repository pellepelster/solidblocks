package de.solidblocks.cloud.providers.github

import de.solidblocks.cloud.providers.ProviderRegistration

class GithubProviderRegistration : ProviderRegistration<GithubProviderConfiguration, GithubProviderRuntime, GithubProviderManager> {

    override val type = GITHUB_PROVIDER_TYPE

    override val supportedConfiguration = GithubProviderConfiguration::class
    override val supportedRuntime = GithubProviderRuntime::class

    override fun createManager() = GithubProviderManager()

    override fun createFactory() = GithubProviderConfigurationFactory()
}
