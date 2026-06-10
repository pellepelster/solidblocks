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
import de.solidblocks.cloud.utils.parseProtonPassList
import de.solidblocks.cloud.utils.parseProtonPassVaults
import de.solidblocks.cloud.utils.protonPassItemCreateNote
import de.solidblocks.cloud.utils.protonPassItemDelete
import de.solidblocks.cloud.utils.protonPassItemList
import de.solidblocks.cloud.utils.protonPassItemView
import de.solidblocks.cloud.utils.protonPassTest
import de.solidblocks.cloud.utils.protonPassVaultCreate
import de.solidblocks.cloud.utils.protonPassVaultList
import de.solidblocks.utils.LogContext
import java.util.*

class ProtonPassProviderManager : ProviderManager<ProtonPassProviderConfiguration, ProtonPassProviderRuntime> {

    override fun validateConfiguration(configuration: ProtonPassProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<ProtonPassProviderRuntime> {
        val vaultName = configuration.vaultName ?: "${context.environment.cloud}-${context.environment.environment}"
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
            log.warning("vault '$vaultName' not found, creating")
            when (val result = protonPassVaultCreate(vaultName).asResult("pass-cli vault create")) {
                is Error<CommandResult> -> return Error(result.error)
                is Success<CommandResult> -> {
                }
            }
        }

        val configCheckSecretTitle = ".blcks-test"

        cleanTestSecrets(vaultName, configCheckSecretTitle, log)

        val configCheckSecret = UUID.randomUUID().toString()

        log.info("verifying proton pass configuration by writing and reading secret '$configCheckSecretTitle'")

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

        cleanTestSecrets(vaultName, configCheckSecretTitle, log)

        if (item.content.note != configCheckSecret) {
            return Error("proton pass setup validation failed, written and read secrets do not match")
        }

        return Success(ProtonPassProviderRuntime(vaultName))
    }

    private fun cleanTestSecrets(vaultName: String, configCheckSecretTitle: String, log: LogContext) {
        protonPassItemList(vaultName)?.let {
            parseProtonPassList(it.stdout)
        }?.items?.filter { it.title == configCheckSecretTitle }?.forEach { item ->
            log.debug("removing secret '${item.title}' (${item.id})")
            protonPassItemDelete(item.shareId, item.id)
        }
    }

    override fun createProvisioners(runtime: ProtonPassProviderRuntime) = listOf(ProtonPassSecretProvisioner(runtime.vaultName))

    override val supportedConfiguration = ProtonPassProviderConfiguration::class
}
