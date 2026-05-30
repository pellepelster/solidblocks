package de.solidblocks.cloud.provisioner.protonpass

import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.interpolation.StringInterpolationFactory
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretProvisioner
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.StaticSecret
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.asResult
import de.solidblocks.cloud.utils.onError
import de.solidblocks.cloud.utils.parseProtonPassItem
import de.solidblocks.cloud.utils.protonPassItemCreateNote
import de.solidblocks.cloud.utils.protonPassItemDelete
import de.solidblocks.cloud.utils.protonPassItemView
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class ProtonPassSecretProvisioner(val vaultName: String) :
    InfrastructureResourceLookupProvider<GenericSecretLookup, GenericSecretRuntime>,
    GenericSecretProvisioner<GenericSecret<GenericSecretRuntime>, GenericSecretRuntime, GenericSecretLookup>,
    StringInterpolationFactory {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: GenericSecret<GenericSecretRuntime>, context: ProvisionerDiffContext): Result<ResourceDiff> {
        val runtime = lookup(resource.asLookup(), context)

        return Success(
            if (runtime != null) {
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
            },
        )
    }

    override suspend fun lookup(lookup: GenericSecretLookup, context: SSHProvisionerContext) = lookupInternal(lookup)

    private fun lookupInternal(lookup: GenericSecretLookup): ProtonPassSecretRuntime? {
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

    override suspend fun apply(resource: GenericSecret<GenericSecretRuntime>, context: ProvisionerApplyContext, log: LogContext): Result<GenericSecretRuntime> {
        val current = lookup(resource.asLookup(), context)

        if (current != null && !context.isTainted(resource) && resource.secretGenerator.isEphemeral()) {
            return Success(current)
        }

        log.debug("creating secret at '${resource.name}' in vault '$vaultName'")
        val secret = resource.secretGenerator.generate(context)

        // proton pass note items cannot be updated via stdin, so an existing item is removed and recreated
        if (current != null) {
            protonPassItemDelete(current.shareId, current.itemId).asResult("pass-cli item delete")
                .onError { return Error(it.error, it.cause) }
        }

        protonPassItemCreateNote(vaultName, resource.name, secret).asResult("pass-cli item create")
            .onError { return Error(it.error, it.cause) }

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error("error creating ${resource.logText()}")
    }

    override val supportedLookupType = GenericSecretLookup::class

    override val supportedResourceType = GenericSecret::class

    override val interpolationType = "secret"

    override fun validate(interpolation: String): Result<Unit> = when (lookupInternal(GenericSecretLookup(interpolation))) {
        is GenericSecretRuntime -> Success(Unit)
        else -> Error("pass secret '$interpolation' does not exist'")
    }

    override fun resolve(interpolation: String): Result<String> = when (val result = lookupInternal(GenericSecretLookup(interpolation))) {
        is GenericSecretRuntime -> Success(result.secret.trim())
        else -> Error("pass secret '$interpolation' does not exist'")
    }
}
