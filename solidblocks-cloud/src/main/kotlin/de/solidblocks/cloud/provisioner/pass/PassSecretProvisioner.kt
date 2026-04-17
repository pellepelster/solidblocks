package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
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
    ResourceLookupProvider<PassSecretLookup, PassSecretRuntime>,
    InfrastructureResourceProvisioner<PassSecret, PassSecretRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: PassSecret, context: CloudProvisionerContext): ResourceDiff {
        val runtime = lookup(resource.asLookup(), context)

        return if (runtime != null) {
            when (val secretGenerator = resource.secretGenerator) {
                is StaticSecret -> if (runtime.secret != secretGenerator.generate(context)) {
                    ResourceDiff(resource, has_changes)
                } else {
                    ResourceDiff(resource, up_to_date)
                }

                else -> ResourceDiff(resource, up_to_date)
            }
        } else {
            ResourceDiff(resource, missing)
        }
    }

    override suspend fun lookup(lookup: PassSecretLookup, context: CloudProvisionerContext): PassSecretRuntime? {
        val result = passShow(lookup.name, passwordStoreDir)

        if (result == null) {
            logger.error { "pass command failed" }
            return null
        }

        if (result.exitCode == 0) {
            return PassSecretRuntime(lookup.name, result.stdout)
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

    override suspend fun apply(resource: PassSecret, context: CloudProvisionerContext, log: LogContext): Result<PassSecretRuntime> {
        val current = lookup(resource.asLookup(), context)

        if (current != null && !resource.tainted && resource.secretGenerator.isEphemeral()) {
            return Success(current)
        }

        log.debug("creating secret at '${resource.name}'")
        val secret = resource.secretGenerator.generate(context)
        when (val result = passInsert(resource.name, secret, passwordStoreDir).asResult("pass insert")) {
            is Error<CommandResult> -> Error<PassSecretRuntime>(result.error)
            is Success<CommandResult> -> {}
        }

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<PassSecretRuntime>("error creating ${resource.logText()}")
    }

    override val supportedLookupType = PassSecretLookup::class

    override val supportedResourceType = PassSecret::class
}
