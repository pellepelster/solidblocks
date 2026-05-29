package de.solidblocks.cloud.services.github.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.ServiceConfigurationFactory
import de.solidblocks.cloud.services.ServiceConfigurationFactory.parseServiceCommonConfig
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result

class GithubRunnerServiceConfigurationFactory : PolymorphicConfigurationFactory<GithubRunnerServiceConfiguration>() {

    val labels =
        StringListKeyword(
            "labels",
            KeywordHelp(
                "list of runner labels used to route workflow jobs to this runner",
            ),
        )

    val packages =
        StringListKeyword(
            "packages",
            KeywordHelp(
                "extra Ubuntu packages to install during machine provisioning",
            ),
        )

    val allowSudo =
        OptionalBooleanKeyword(
            "allow_sudo",
            KeywordHelp(
                "allow password-less sudo commands for the GitHub runner user",
            ),
            false,
        )

    val scale =
        NumberKeywordOptionalWithDefault(
            "scale",
            NumberConstraints(10, 0),
            KeywordHelp(
                "Number if runner instances to create",
            ),
            1,
        )

    override val help =
        ConfigurationHelp(
            "GitHub Runner",
            "Provisions a self-hosted runner based on the configured cloud provider. " +
                "Requires a `github` provider to be configured with the target organisation or repository URL " +
                "and a personal access token supplied via the `GITHUB_TOKEN` environment variable.",
        )

    override val keywords =
        listOf(labels, scale, packages, allowSudo) + ServiceConfigurationFactory.keywords

    override fun parse(yaml: YamlNode): Result<GithubRunnerServiceConfiguration> = result {
        GithubRunnerServiceConfiguration(
            yaml.parseServiceCommonConfig().bind(),
            labels.parse(yaml).bind(),
            packages.parse(yaml).bind(),
            allowSudo.parse(yaml).bind(),
            scale.parse(yaml).bind(),
        )
    }
}
