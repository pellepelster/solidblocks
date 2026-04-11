package de.solidblocks.cloud.providers.backup.aws

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.StringConstraints.Companion.NONE
import de.solidblocks.cloud.configuration.StringKeywordOptionalWithDefault
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.services.PROVIDER_NAME_KEYWORD
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class S3BackupProviderConfigurationFactory : PolymorphicConfigurationFactory<S3BackupProviderConfiguration>() {

    val region =
        StringKeywordOptionalWithDefault(
            "region",
            NONE,
            "eu-central-1",
            KeywordHelp(
                "Region where the backup bucket should be created",
            ),

        )

    override val help =
        ConfigurationHelp(
            "Backup S3",
            "Provides backup of cloud data to AWS S3 buckets. During plan/apply the `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must be set with credentials that have the permission to create new S3 Buckets, as well as IAM users and access keys. For each service a dedicated backup bucket and separate IAM credentials will be created.",
        )

    override val keywords = listOf(PROVIDER_NAME_KEYWORD, region)

    override fun parse(yaml: YamlNode): Result<S3BackupProviderConfiguration> {
        val name =
            when (val result = PROVIDER_NAME_KEYWORD.parse(yaml)) {
                is Error<String> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val region =
            when (val result = region.parse(yaml)) {
                is Error<String> -> return Error(result.error)
                is Success<String> -> result.data
            }

        return Success(S3BackupProviderConfiguration(name, region))
    }
}
