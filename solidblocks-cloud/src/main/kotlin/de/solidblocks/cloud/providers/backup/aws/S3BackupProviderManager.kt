package de.solidblocks.cloud.providers.backup.aws

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfigurationManager
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getEnvOrProperty
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class S3BackupProviderManager :
    BackupProviderConfigurationManager<S3BackupProviderConfiguration, S3BackupProviderConfigurationRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun validate(configuration: S3BackupProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<S3BackupProviderConfigurationRuntime> {
        if (getEnvOrProperty("AWS_ACCESS_KEY_ID") == null) {
            return Error("environment variable 'AWS_ACCESS_KEY_ID' not set")
        }

        if (getEnvOrProperty("AWS_SECRET_ACCESS_KEY") == null) {
            return Error("environment variable 'AWS_SECRET_ACCESS_KEY' not set")
        }

        return Success(S3BackupProviderConfigurationRuntime(configuration.region, getEnvOrProperty("AWS_ACCESS_KEY_ID"), getEnvOrProperty("AWS_SECRET_ACCESS_KEY")))
    }

    override fun createProvisioners(runtime: S3BackupProviderConfigurationRuntime) = emptyList<InfrastructureResourceProvisioner<*, *>>()

    override val supportedConfiguration = S3BackupProviderConfiguration::class
}
