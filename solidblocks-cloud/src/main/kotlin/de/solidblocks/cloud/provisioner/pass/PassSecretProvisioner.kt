package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.utils.runCommand
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import io.github.oshai.kotlinlogging.KotlinLogging

class PassSecretProvisioner(val path: String? = null) :
    ResourceLookupProvider<PassSecretLookup, PassSecretRuntime>,
    InfrastructureResourceProvisioner<PassSecret, PassSecretRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: PassSecret, context: ProvisionerContext): ResourceDiff? {
        val runtime = lookup(resource.asLookup(), context)

        return if (runtime != null) {
            ResourceDiff(resource, up_to_date)
        } else {
            ResourceDiff(resource, missing)
        }
    }

    override suspend fun lookup(lookup: PassSecretLookup, context: ProvisionerContext): PassSecretRuntime? {
        val result = runCommand(listOf("pass", "show", lookup.name))

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

    private fun generateSecret(length: Int, allowedChars: List<Char>) =
        (1..length).map { allowedChars.random() }.joinToString("")

    override suspend fun apply(
        resource: PassSecret,
        context: ProvisionerContext,
        log: LogContext,
    ): ApplyResult<PassSecretRuntime> {
        val current = lookup(resource.asLookup(), context)

        if (current != null && !resource.tainted) {
            return ApplyResult(current)
        }

        logDebug("creating secret at '${resource.name}'", context = log)

        val secret = if (resource.secret == null) {
            generateSecret(resource.length, resource.allowedChars)
        } else {
            resource.secret(context)
        }

        val result =
            runCommand(
                listOf("pass", "insert", "--multiline", "--force", resource.name),
                secret,
            )

        if (result == null || result.exitCode != 0) {
            logger.error { "pass insert command failed" }
            return ApplyResult(null)
        }

        return ApplyResult(lookup(resource.asLookup(), context))
    }

    override val supportedLookupType = PassSecretLookup::class

    override val supportedResourceType = PassSecret::class
}
