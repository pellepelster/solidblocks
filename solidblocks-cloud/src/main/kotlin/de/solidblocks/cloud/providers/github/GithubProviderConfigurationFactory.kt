package de.solidblocks.cloud.providers.github

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.StringListKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class GithubProviderConfigurationFactory : PolymorphicConfigurationFactory<GithubProviderConfiguration>() {

    val githubUrl =
        StringKeyword(
            "github_url",
            NONE,
            KeywordHelp("GitHub URL scoping the runner, either an organisation (https://github.com/<org>) or a repository (https://github.com/<user>/<repo>)"),
        )

    override val help =
        ConfigurationHelp(
            "GitHub",
            "Provides GitHub Actions runner support. A personal access token with the required runner registration permissions must be supplied via the environment variable `GITHUB_TOKEN`.",
        )

    val labels =
        StringListKeyword(
            "labels",
            KeywordHelp("Labels for the runner"),
        )

    override val keywords = listOf(PROVIDER_NAME_KEYWORD, githubUrl, labels)

    override fun parse(yaml: YamlNode): Result<GithubProviderConfiguration> {
        val name =
            when (val result = PROVIDER_NAME_KEYWORD.parse(yaml)) {
                is Error<String> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val githubUrl =
            when (val result = githubUrl.parse(yaml)) {
                is Error<String> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val labels =
            when (val result = labels.parse(yaml)) {
                is Error<List<String>> -> return Error(result.error)
                is Success<List<String>> -> result.data
            }

        return Success(GithubProviderConfiguration(name, githubUrl))
    }
}
