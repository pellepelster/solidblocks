package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.interpolation.StringInterpolationFactory
import de.solidblocks.cloud.providers.pass.PASS_PROVIDER_TYPE
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretProvisioner
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.StaticSecret
import de.solidblocks.cloud.utils.CommandResult
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.asResult
import de.solidblocks.cloud.utils.passInsert
import de.solidblocks.cloud.utils.passShow
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class PassSecretProvisioner(val passwordStoreDir: String) :
    InfrastructureResourceLookupProvider<GenericSecretLookup, GenericSecretRuntime>,
    GenericSecretProvisioner<GenericSecret<GenericSecretRuntime>, GenericSecretRuntime, GenericSecretLookup>,
    StringInterpolationFactory {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: GenericSecret<GenericSecretRuntime>, context: ProvisionerDiffContext): ResourceDiff {
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

    override suspend fun lookup(lookup: GenericSecretLookup, context: SSHProvisionerContext) = lookupInternal(lookup)

    override suspend fun apply(resource: GenericSecret<GenericSecretRuntime>, context: ProvisionerApplyContext, log: LogContext): Result<GenericSecretRuntime> {
        val current = lookup(resource.asLookup(), context)

        if (current != null && !resource.isTainted() && resource.secretGenerator.isEphemeral()) {
            return Success(current)
        }

        log.debug("creating secret at '${resource.name}'")
        val secret = resource.secretGenerator.generate(context)
        when (val result = passInsert(resource.name, secret, passwordStoreDir).asResult("pass insert")) {
            is Error<CommandResult> -> Error<GenericSecretRuntime>(result.error)
            is Success<CommandResult> -> {}
        }

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<GenericSecretRuntime>("error creating ${resource.logText()}")
    }

    private fun lookupInternal(lookup: GenericSecretLookup): GenericSecretRuntime? {
        val result = passShow(lookup.name, passwordStoreDir)

        if (result == null) {
            logger.error { "pass command failed" }
            return null
        }

        if (result.exitCode == 0) {
            return GenericSecretRuntime(lookup.name, result.stdout)
        } else {
            if (result.stdout.contains("is not in the password store")) {
                return null
            }
        }

        logger.error {
            "invalid pass command result (${result.exitCode}), stdout: '${result.stdout}', stderr: '${result.stderr}'"
        }
        return null
    }

    override val interpolationType = PASS_PROVIDER_TYPE

    override fun validate(interpolation: String): Result<Unit> = when (lookupInternal(GenericSecretLookup(interpolation))) {
        is GenericSecretRuntime -> Success(Unit)
        else -> Error("pass secret '$interpolation' does not exist'")
    }

    override fun resolve(interpolation: String): Result<String> = when (val result = lookupInternal(GenericSecretLookup(interpolation))) {
        is GenericSecretRuntime -> Success(result.secret.trim())
        else -> Error("pass secret '$interpolation' does not exist'")
    }

    override val supportedLookupType = GenericSecretLookup::class

    override val supportedResourceType = GenericSecret::class
}
