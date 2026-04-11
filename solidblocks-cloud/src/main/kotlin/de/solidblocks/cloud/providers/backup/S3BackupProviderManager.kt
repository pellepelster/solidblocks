package de.solidblocks.cloud.providers.backup

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderConfigurationFactory.Companion.defaultSSHKeyNames
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
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
    ProviderConfigurationManager<
        S3BackupProviderConfiguration,
        S3BackupProviderRuntime,
        > {

    private val logger = KotlinLogging.logger {}

    override fun validate(configuration: S3BackupProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<S3BackupProviderRuntime> =
        Success(S3BackupProviderRuntime(configuration.region, "TODO", "TODO"))

    private fun checkFilePermission(sshKey: Path): Boolean {
        val permissions = Files.getPosixFilePermissions(sshKey)

        val allowedPermissions =
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
            )

        return !permissions.any { it !in allowedPermissions }
    }

    fun expandTilde(path: String): String = if (path.startsWith("~")) {
        System.getProperty("user.home") + path.substring(1)
    } else {
        path
    }

    override fun createProvisioners(runtime: S3BackupProviderRuntime) = emptyList<InfrastructureResourceProvisioner<*, *>>()

    override val supportedConfiguration = S3BackupProviderConfiguration::class
}
