package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.providers.ConfigurationContext
import de.solidblocks.cloud.providers.ssh.SSHKeyProviderConfigurationManager
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import de.solidblocks.utils.logInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText

class LocalSSHKeyProviderManager :
    SSHKeyProviderConfigurationManager<
            LocalSSHKeyProviderConfiguration,
            LocalSSHKeyProviderRuntime,
            > {

    private val logger = KotlinLogging.logger {}

    val defaultSSHKeyNames =
        listOf(
            "id_rsa",
            "id_ecdsa",
            "id_ecdsa_sk",
            "id_ed25519",
            "id_ed25519_sk",
        )

    val homeDir = Path(System.getProperty("user.home"))

    override fun validate(
        configuration: LocalSSHKeyProviderConfiguration,
        log: LogContext,
        context: ConfigurationContext,
    ): Result<LocalSSHKeyProviderRuntime> {
        val sshKey =
            when (val result = tryFindKey(configuration, context)) {
                is Error<*> -> return Error<LocalSSHKeyProviderRuntime>(result.error)
                is Success<Path> -> result.data
            }

        logDebug("found ssh key at '${sshKey.toAbsolutePath()}'")

        if (SSHKeyUtils.isEncrypted(sshKey.readText())) {
            return Error("encrypted private keys '$sshKey' are currently not supported")
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

        logInfo(
            "found ssh private key at '$sshKey' with type '${sshKeyPair.private.algorithm.lowercase()}'",
            context = log,
        )

        if (!checkFilePermission(sshKey)) {
            return Error("permissions for ssh key '${sshKey.absolutePathString()}' are too open, should be owner r/w only")
        }

        return Success(LocalSSHKeyProviderRuntime(sshKeyPair, sshKey.toAbsolutePath()))
    }

    private fun checkFilePermission(sshKey: Path): Boolean {
        val permissions = Files.getPosixFilePermissions(sshKey)

        val allowedPermissions = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
        )

        return !permissions.any { it !in allowedPermissions }
    }


    private fun tryFindKey(
        configuration: LocalSSHKeyProviderConfiguration,
        context: ConfigurationContext,
    ): Result<Path> {
        if (configuration.privateKey != null) {
            val sshKeyFile = context.configFilePath.toAbsolutePath().resolve(configuration.privateKey)

            return if (sshKeyFile.exists()) {
                Success(sshKeyFile)
            } else {
                Error("ssh key file '${sshKeyFile.absolutePathString()}' does not exist")
            }
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

    override fun createProvisioners(runtime: LocalSSHKeyProviderRuntime) =
        emptyList<InfrastructureResourceProvisioner<*, *>>()

    override val supportedConfiguration = LocalSSHKeyProviderConfiguration::class
}
