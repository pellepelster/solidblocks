package de.solidblocks.cloud.providers.github

import de.solidblocks.cloud.github.GitHubApi
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getPropertyOrEnv
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

class GithubProviderManager : ProviderManager<GithubProviderConfiguration, GithubProviderRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun validateConfiguration(configuration: GithubProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<GithubProviderRuntime> {
        val githubToken = getPropertyOrEnv("GITHUB_TOKEN")

        if (githubToken == null) {
            return "environment variable 'GITHUB_TOKEN' not set".also {
                log.error(it)
            }.let { Error(it) }
        }

        try {
            runBlocking { GitHubApi(githubToken).rateLimit() }
            log.info("provided GitHub token is valid")
        } catch (e: Exception) {
            return "provided GitHub token is not valid".also {
                logger.error(e) { it }
                log.error(it)
            }.let { Error(it) }
        }

        log.info("GitHub provider configured for '${configuration.githubUrl}'")

        val cleaned = configuration.githubUrl
            .trim()
            .removeSuffix("/")
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("github.com/")

        val parts = cleaned.split("/").filter { it.isNotBlank() }

        val gitHubUrl = when (parts.size) {
            1 -> GitHubUrlRuntime.Organization(parts[0])
            2 -> GitHubUrlRuntime.Repository(username = parts[0], repo = parts[1])
            else -> return Error("'${configuration.githubUrl}' is not a valid github url")
        }

        return Success(GithubProviderRuntime(gitHubUrl, githubToken))
    }

    override fun createProvisioners(runtime: GithubProviderRuntime) = emptyList<Nothing>()

    override val supportedConfiguration = GithubProviderConfiguration::class
}
