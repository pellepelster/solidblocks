package de.solidblocks.cloud.providers.github

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime

sealed class GitHubUrlRuntime {
    data class Organization(val org: String) : GitHubUrlRuntime() {
        override fun toUrl() = "https://github.com/$org"
    }

    data class Repository(val username: String, val repo: String) : GitHubUrlRuntime() {
        override fun toUrl() = "https://github.com/$username/$repo"
    }

    abstract fun toUrl(): String
}

data class GithubProviderRuntime(val githubUrl: GitHubUrlRuntime, val githubToken: String) : ProviderConfigurationRuntime
