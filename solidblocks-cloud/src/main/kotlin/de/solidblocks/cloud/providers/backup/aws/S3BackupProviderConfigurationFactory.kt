package de.solidblocks.cloud.providers.backup.aws

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.result

class S3BackupProviderConfigurationFactory : PolymorphicConfigurationFactory<S3BackupProviderConfiguration>() {

    val region =
        StringKeyword(
            "region",
            KeywordHelp(
                "Region where the backup bucket should be created",
            ),
        ).default("eu-central-1")

    override val help =
        ConfigurationHelp(
            "Backup S3",
            "Uses AWS S3 buckets for service backups. During plan/apply the `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must be set with credentials that have the permission to create new S3 Buckets, as well as IAM users and access keys. For each service a dedicated backup bucket and separate IAM credentials will be created.",
        )

    override val keywords = listOf(PROVIDER_NAME_KEYWORD, region)

    override fun parse(yaml: YamlNode): Result<S3BackupProviderConfiguration> = result {
        S3BackupProviderConfiguration(
            PROVIDER_NAME_KEYWORD.parse(yaml).bind(),
            region.parse(yaml).bind(),
        )
    }
}
