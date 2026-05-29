package de.solidblocks.cloud.provisioner.protonpass

import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.secret.SecretProvisioner
import de.solidblocks.cloud.provisioner.secret.StaticSecret
import de.solidblocks.cloud.utils.CommandResult
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.asResult
import de.solidblocks.cloud.utils.parseProtonPassItem
import de.solidblocks.cloud.utils.protonPassItemCreateNote
import de.solidblocks.cloud.utils.protonPassItemDelete
import de.solidblocks.cloud.utils.protonPassItemView
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class ProtonPassSecretProvisioner(val vaultName: String) :
    InfrastructureResourceLookupProvider<ProtonPassSecretLookup, ProtonPassSecretRuntime>,
    SecretProvisioner<ProtonPassSecret, ProtonPassSecretRuntime, ProtonPassSecretLookup> {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: ProtonPassSecret, context: ProvisionerDiffContext): ResourceDiff {
        val runtime = lookup(resource.asLookup(), context)

        return if (runtime != null) {
            when (val secretGenerator = resource.secretGenerator) {
                is StaticSecret -> {
                    val secret = try {
                        secretGenerator.generate(context)
                    } catch (e: Exception) {
                        logger.error(e) { "failed to generate secret" }
                        null
                    }

                    if (secret == null) {
                        ResourceDiff(resource, unknown)
                    } else if (runtime.secret != secret) {
                        ResourceDiff(resource, has_changes)
                    } else {
                        ResourceDiff(resource, up_to_date)
                    }
                }

                else -> ResourceDiff(resource, up_to_date)
            }
        } else {
            ResourceDiff(resource, missing)
        }
    }

    override suspend fun lookup(lookup: ProtonPassSecretLookup, context: SSHProvisionerContext): ProtonPassSecretRuntime? {
        val result = protonPassItemView(vaultName, lookup.name)

        if (result == null) {
            logger.error { "pass-cli command failed" }
            return null
        }

        if (result.exitCode == 0) {
            val item = parseProtonPassItem(result.stdout)
            if (item == null) {
                logger.error { "failed to parse pass-cli item view output" }
                return null
            }
            return ProtonPassSecretRuntime(lookup.name, item.content.note ?: "", item.shareId, item.id)
        }

        if (result.stderr.contains("No item found")) {
            return null
        }

        logger.error {
            "invalid pass-cli command result (${result.exitCode}), stdout: '${result.stdout}', stderr: '${result.stderr}'"
        }
        return null
    }

    override suspend fun apply(resource: ProtonPassSecret, context: ProvisionerApplyContext, log: LogContext): Result<ProtonPassSecretRuntime> {
        val current = lookup(resource.asLookup(), context)

        if (current != null && !resource.tainted && resource.secretGenerator.isEphemeral()) {
            return Success(current)
        }

        log.debug("creating secret at '${resource.name}' in vault '$vaultName'")
        val secret = resource.secretGenerator.generate(context)

        // proton pass note items cannot be updated via stdin, so an existing item is removed and recreated
        if (current != null) {
            when (val result = protonPassItemDelete(current.shareId, current.itemId).asResult("pass-cli item delete")) {
                is Error<CommandResult> -> return Error(result.error)
                is Success<CommandResult> -> {}
            }
        }

        when (val result = protonPassItemCreateNote(vaultName, resource.name, secret).asResult("pass-cli item create")) {
            is Error<CommandResult> -> return Error(result.error)
            is Success<CommandResult> -> {}
        }

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error("error creating ${resource.logText()}")
    }

    override val lookupType = ProtonPassSecretLookup::class

    override val resourceType = ProtonPassSecret::class
}
