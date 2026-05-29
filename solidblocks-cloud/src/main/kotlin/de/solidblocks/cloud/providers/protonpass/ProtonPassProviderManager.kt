package de.solidblocks.cloud.providers.protonpass

import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.provisioner.protonpass.ProtonPassSecretProvisioner
import de.solidblocks.cloud.utils.CommandResult
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.asResult
import de.solidblocks.cloud.utils.commandExists
import de.solidblocks.cloud.utils.getEnvOrProperty
import de.solidblocks.cloud.utils.parseProtonPassItem
import de.solidblocks.cloud.utils.parseProtonPassVaults
import de.solidblocks.cloud.utils.protonPassItemCreateNote
import de.solidblocks.cloud.utils.protonPassItemDelete
import de.solidblocks.cloud.utils.protonPassItemView
import de.solidblocks.cloud.utils.protonPassTest
import de.solidblocks.cloud.utils.protonPassVaultList
import de.solidblocks.utils.LogContext
import java.util.*

class ProtonPassProviderManager : ProviderManager<ProtonPassProviderConfiguration, ProtonPassProviderRuntime> {

    override fun validateConfiguration(configuration: ProtonPassProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<ProtonPassProviderRuntime> {
        val vaultName = configuration.vaultName ?: context.environment.cloud
        log.info("using proton pass vault '$vaultName'")

        if (getEnvOrProperty("BLCKS_PROTONPASS_PROVIDER_SKIP_VALIDATION") != null) {
            log.info("skipping proton pass provider validation, using vault '$vaultName'")
            return Success(ProtonPassProviderRuntime(vaultName))
        }

        if (commandExists("pass-cli")) {
            log.debug("found 'pass-cli' executable")
        } else {
            "'pass-cli' executable not found"
                .also {
                    log.error(it)
                    return Error<ProtonPassProviderRuntime>(it)
                }
        }

        log.info("verifying authenticated connection to proton pass")
        when (val result = protonPassTest().asResult("pass-cli test")) {
            is Error<CommandResult> -> return Error(result.error)
            is Success<CommandResult> -> {}
        }

        val vaults = when (val result = protonPassVaultList().asResult("pass-cli vault list")) {
            is Error<CommandResult> -> return Error(result.error)
            is Success<CommandResult> ->
                parseProtonPassVaults(result.data.stdout)
                    ?: return Error("failed to parse 'pass-cli vault list' output")
        }

        if (vaults.none { it.name == vaultName }) {
            return Error("proton pass vault '$vaultName' does not exist")
        }

        val configCheckSecretTitle = ".blcks-test"
        val configCheckSecret = UUID.randomUUID().toString()

        log.info("verifying proton pass configuration by writing and reading secret '$configCheckSecretTitle'")

        // remove a possibly left over test secret from a previous run
        protonPassItemView(vaultName, configCheckSecretTitle)?.let { existing ->
            if (existing.exitCode == 0) {
                parseProtonPassItem(existing.stdout)?.let { protonPassItemDelete(it.shareId, it.id) }
            }
        }

        when (val result = protonPassItemCreateNote(vaultName, configCheckSecretTitle, configCheckSecret).asResult("proton pass setup validation write")) {
            is Error<CommandResult> -> return Error(result.error)
            is Success<CommandResult> -> {}
        }

        val item = when (val result = protonPassItemView(vaultName, configCheckSecretTitle).asResult("proton pass setup validation read")) {
            is Error<CommandResult> -> return Error(result.error)
            is Success<CommandResult> ->
                parseProtonPassItem(result.data.stdout)
                    ?: return Error("failed to parse proton pass setup validation read")
        }

        // clean up the test secret again
        protonPassItemDelete(item.shareId, item.id)

        if (item.content.note != configCheckSecret) {
            return Error("proton pass setup validation failed, written and read secrets do not match")
        }

        return Success(ProtonPassProviderRuntime(vaultName))
    }

    override fun createProvisioners(runtime: ProtonPassProviderRuntime) = listOf(ProtonPassSecretProvisioner(runtime.vaultName))

    override val supportedConfiguration = ProtonPassProviderConfiguration::class
}
