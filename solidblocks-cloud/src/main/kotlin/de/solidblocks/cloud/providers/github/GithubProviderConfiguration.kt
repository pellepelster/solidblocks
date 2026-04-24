package de.solidblocks.cloud.providers.github

import de.solidblocks.cloud.providers.ProviderConfiguration

const val GITHUB_PROVIDER_TYPE = "github"

data class GithubProviderConfiguration(override val name: String, val githubUrl: String) : ProviderConfiguration {
    override val type = GITHUB_PROVIDER_TYPE
}
