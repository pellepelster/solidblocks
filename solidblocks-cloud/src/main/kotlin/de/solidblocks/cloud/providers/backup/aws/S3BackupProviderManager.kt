package de.solidblocks.cloud.providers.backup.aws

import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.configuration.model.EnvironmentReference
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfigurationManager
import de.solidblocks.cloud.provisioner.aws.iam.AwsIamUser
import de.solidblocks.cloud.provisioner.aws.iam.AwsIamUserProvisioner
import de.solidblocks.cloud.provisioner.aws.s3.AwsS3BucketProvisioner
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getEnvOrProperty
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class S3BackupProviderManager :
    BackupProviderConfigurationManager<S3BackupProviderConfiguration, S3BackupProviderConfigurationRuntime> {

    private val logger = KotlinLogging.logger {}

    companion object {
        fun secretKeySecretPath(environment: EnvironmentReference, userName: String) = secretPath(environment, listOf("aws", "users", userName, "secret_key"))

        fun accessKeySecretPath(environment: EnvironmentReference, userName: String) = secretPath(environment, listOf("aws", "users", userName, "access_key"))

        fun bucketName(environment: EnvironmentReference, runtime: ServiceConfigurationRuntime) = "${environment.cloud}-${environment.environment}-${runtime.name}-backup"

        fun iamUserName(environment: EnvironmentReference, runtime: ServiceConfigurationRuntime) = "${environment.cloud}-${environment.environment}-${runtime.name}"
        fun backupBucketPolicy(bucketArn: String) = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
              "Resource": ["$bucketArn", "$bucketArn/*"]
            }
          ]
        }
        """.trimIndent()
    }

    override fun validate(configuration: S3BackupProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<S3BackupProviderConfigurationRuntime> {
        if (getEnvOrProperty("AWS_ACCESS_KEY_ID") == null) {
            return Error("environment variable 'AWS_ACCESS_KEY_ID' not set")
        }

        if (getEnvOrProperty("AWS_SECRET_ACCESS_KEY") == null) {
            return Error("environment variable 'AWS_SECRET_ACCESS_KEY' not set")
        }

        return Success(S3BackupProviderConfigurationRuntime(configuration.region, getEnvOrProperty("AWS_ACCESS_KEY_ID"), getEnvOrProperty("AWS_SECRET_ACCESS_KEY")))
    }

    override fun createProvisioners(runtime: S3BackupProviderConfigurationRuntime) = listOf(
        AwsS3BucketProvisioner(runtime.accessKey, runtime.secretKey, runtime.region),
        AwsIamUserProvisioner(runtime.accessKey, runtime.secretKey)
    ) as List<InfrastructureResourceProvisioner<*, *>>

    override val supportedConfiguration = S3BackupProviderConfiguration::class
}
