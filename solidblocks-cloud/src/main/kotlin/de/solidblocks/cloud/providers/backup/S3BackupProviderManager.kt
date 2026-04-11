package de.solidblocks.cloud.providers.backup

import com.sun.tools.javac.jvm.ByteCodes
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderConfigurationFactory.Companion.defaultSSHKeyNames
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getEnvOrProperty
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText

class S3BackupProviderManager :
    ProviderConfigurationManager<S3BackupProviderConfiguration, S3BackupProviderRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun validate(configuration: S3BackupProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<S3BackupProviderRuntime> {

        if (getEnvOrProperty("AWS_ACCESS_KEY_ID") == null) {
            return Error("environment variable 'AWS_ACCESS_KEY_ID' not set")
        }

        if (getEnvOrProperty("AWS_SECRET_ACCESS_KEY") == null) {
            return Error("environment variable 'AWS_SECRET_ACCESS_KEY' not set")
        }

        return Success(S3BackupProviderRuntime(configuration.region, getEnvOrProperty("AWS_ACCESS_KEY_ID"), getEnvOrProperty("AWS_SECRET_ACCESS_KEY")))
    }

    override fun createProvisioners(runtime: S3BackupProviderRuntime) = emptyList<InfrastructureResourceProvisioner<*, *>>()

    override val supportedConfiguration = S3BackupProviderConfiguration::class
}
