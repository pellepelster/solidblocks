package de.solidblocks.cloud.providers.github

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result

class GithubProviderConfigurationFactory : PolymorphicConfigurationFactory<GithubProviderConfiguration>() {

    val githubUrl =
        StringKeyword(
            "github_url",
            NONE,
            KeywordHelp("GitHub URL, either an organisation (https://github.com/<org>) or a repository (https://github.com/<user>/<repo>)"),
        )

    override val help =
        ConfigurationHelp(
            "GitHub",
            "Provides integration of GitHub resources. A personal access token with appropriate permissions must be supplied via the environment variable `GITHUB_TOKEN`.",
        )

    override val keywords = listOf(PROVIDER_NAME_KEYWORD, githubUrl)

    override fun parse(yaml: YamlNode): Result<GithubProviderConfiguration> = result {
        GithubProviderConfiguration(
            PROVIDER_NAME_KEYWORD.parse(yaml).bind(),
            githubUrl.parse(yaml).bind(),
        )
    }
}
