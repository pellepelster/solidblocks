package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderConfigurationFactory.Companion.defaultSSHKeyNames
import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderManager
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

class LocalSSHKeyProviderManager :
    SSHKeyProviderManager<
        LocalSSHKeyProviderConfiguration,
        LocalSSHKeyProviderConfigurationRuntime,
        > {

    private val logger = KotlinLogging.logger {}

    val homeDir = Path(System.getProperty("user.home"))

    override fun validateConfiguration(configuration: LocalSSHKeyProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<LocalSSHKeyProviderConfigurationRuntime> {
        val sshKey =
            when (val result = tryFindKey(configuration, context, log)) {
                is Error<*> -> return Error<LocalSSHKeyProviderConfigurationRuntime>(result.error)
                is Success<Path> -> result.data
            }

        log.debug("found ssh key at '${sshKey.toAbsolutePath()}'")

        if (SSHKeyUtils.isEncrypted(sshKey.readText())) {
            return Error("encrypted private keys are currently not supported ($sshKey)")
        }

        val sshKeyPair =
            try {
                SSHKeyUtils.tryLoadKey(sshKey.readText())
            } catch (e: Exception) {
                logger.error(e) { "failed to load private key from '$sshKey'" }
                null
            }

        if (sshKeyPair == null) {
            return Error("failed to load private key from '$sshKey'")
        }

        log.info("found ssh private key at '$sshKey' with type '${sshKeyPair.private.algorithm.lowercase()}'")

        if (!checkFilePermission(sshKey)) {
            return Error(
                "permissions for ssh key '${sshKey.absolutePathString()}' are too open, should be owner r/w only",
            )
        }

        return Success(LocalSSHKeyProviderConfigurationRuntime(sshKeyPair, sshKey.toAbsolutePath()))
    }

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

    private fun tryFindKey(configuration: LocalSSHKeyProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<Path> {
        if (configuration.privateKey != null) {
            val sshKeyFile =
                context.configFileDirectory
                    .toAbsolutePath()
                    .resolve(expandTilde(configuration.privateKey))

            return if (sshKeyFile.exists()) {
                Success(sshKeyFile)
            } else {
                Error("ssh key file '${sshKeyFile.absolutePathString()}' does not exist")
            }
        }

        val sshKeyFile =
            context.configFileDirectory
                .toAbsolutePath()
                .resolve(expandTilde("${context.cloudName}.key"))

        if (sshKeyFile.exists()) {
            log.info("found ssh key '${sshKeyFile.absolutePathString()}'")
            return Success(sshKeyFile)
        }

        val sshKey =
            defaultSSHKeyNames.firstNotNullOfOrNull {
                val privateKey = homeDir.resolve(".ssh").resolve(it)

                if (privateKey.exists()) {
                    privateKey
                } else {
                    null
                }
            }

        if (sshKey == null) {
            return Error(
                "no SSH key found, tried ${
                    defaultSSHKeyNames.joinToString(", ") {
                        "'${
                            homeDir.resolve(".ssh").resolve(it)
                        }'"
                    }
                }",
            )
        }

        return Success(sshKey)
    }

    override fun createProvisioners(runtime: LocalSSHKeyProviderConfigurationRuntime) = emptyList<InfrastructureResourceProvisioner<*, *>>()

    override val supportedConfiguration = LocalSSHKeyProviderConfiguration::class
}
