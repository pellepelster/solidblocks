package de.solidblocks.cloud.services.github.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeywordOptional
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.InstanceConfigurationFactory
import de.solidblocks.cloud.services.SERVICE_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class GithubRunnerServiceConfigurationFactory : PolymorphicConfigurationFactory<GithubRunnerServiceConfiguration>() {

    val labels =
        StringKeywordOptional(
            "labels",
            NONE,
            KeywordHelp(
                "Comma-separated list of runner labels used to route workflow jobs to this runner (e.g. `self-hosted,linux,x64`).",
            ),
        )

    override val help =
        ConfigurationHelp(
            "GitHub Runner",
            "Provisions a Hetzner VM and registers it as a GitHub Actions self-hosted runner. " +
                "Requires a `github` provider to be configured with the target organisation or repository URL " +
                "and a personal access token supplied via the `GITHUB_TOKEN` environment variable.",
        )

    override val keywords =
        listOf(SERVICE_NAME_KEYWORD, labels) + InstanceConfigurationFactory.keywords

    override fun parse(yaml: YamlNode): Result<GithubRunnerServiceConfiguration> {
        val name =
            when (val result = SERVICE_NAME_KEYWORD.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val labels =
            when (val result = labels.parse(yaml)) {
                is Error<*> -> return Error(result.error)
                is Success<String?> -> result.data ?: ""
            }

        val instance =
            when (val result = InstanceConfigurationFactory.parse(yaml)) {
                is Error<InstanceConfig> -> return Error(result.error)
                is Success<InstanceConfig> -> result.data
            }

        return Success(GithubRunnerServiceConfiguration(name, labels, instance))
    }
}
