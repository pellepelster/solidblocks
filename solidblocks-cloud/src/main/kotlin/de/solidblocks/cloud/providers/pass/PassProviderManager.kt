package de.solidblocks.cloud.providers.pass

import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.utils.*
import de.solidblocks.utils.LogContext
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists

class PassProviderManager : ProviderManager<PassProviderConfiguration, PassProviderRuntime> {

    override fun validateConfiguration(configuration: PassProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<PassProviderRuntime> {
        if (commandExists("pass")) {
            log.debug("found 'pass' executable")
        } else {
            "'pass' executable not found"
                .also {
                    log.error(it)
                    return Error<PassProviderRuntime>(it)
                }
        }

        val configCheckSecretPath = ".blcks-test"
        val configCheckSecret = UUID.randomUUID().toString()

        val passwordStoreDir = if (configuration.passwordStoreDir == null) {
            log.info("no password store directory configured, using default '${DEFAULT_PASS_DIR}'")
            DEFAULT_PASS_DIR
        } else {
            log.info("using password store directory '${configuration.passwordStoreDir}'")

            if (!Path(configuration.passwordStoreDir).exists()) {
                return Error("password store directory '${configuration.passwordStoreDir}' does not exist")
            }

            configuration.passwordStoreDir
        }

        log.info("verifying pass configuration by writing and reading secret '$configCheckSecretPath'")

        when (val result = passInsert(configCheckSecretPath, configCheckSecret, passwordStoreDir).asResult("pass setup validation write")) {
            is Error<CommandResult> -> return Error(result.error)
            is Success<CommandResult> -> {}
        }

        val stdout = when (val result = passShow(configCheckSecretPath, passwordStoreDir).asResult("pass setup validation read")) {
            is Error<CommandResult> -> return Error(result.error)
            is Success<CommandResult> -> result.data.stdout
        }

        if (stdout != configCheckSecret) {
            return Error("pass setup validation failed, written and read secrets do not match")
        }

        return Success(PassProviderRuntime(passwordStoreDir))
    }

    override fun createProvisioners(runtime: PassProviderRuntime) = listOf(PassSecretProvisioner(runtime.passwordStoreDir))

    override val supportedConfiguration = PassProviderConfiguration::class
}
