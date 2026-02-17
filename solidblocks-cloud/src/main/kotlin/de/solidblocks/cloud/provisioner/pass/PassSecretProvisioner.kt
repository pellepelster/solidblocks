package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.api.resources.Resource
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.utils.runCommand
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import io.github.oshai.kotlinlogging.KotlinLogging

class PassSecretProvisioner(val path: String? = null) :
    ResourceLookupProvider<SecretLookup, SecretRuntime>,
    InfrastructureResourceProvisioner<Secret, SecretRuntime> {

  private val logger = KotlinLogging.logger {}

  fun secretName(resource: Resource, context: ProvisionerContext) =
      "${context.cloudName}/${context.environmentName}/${resource.name}"

  override suspend fun diff(resource: Secret, context: ProvisionerContext): ResourceDiff? {
    val runtime = lookup(resource.asLookup(), context)

    return if (runtime != null) {
      ResourceDiff(resource, up_to_date)
    } else {
      ResourceDiff(resource, missing)
    }
  }

  override suspend fun lookup(lookup: SecretLookup, context: ProvisionerContext): SecretRuntime? {
    val result = runCommand(listOf("pass", "show", secretName(lookup, context)))

    if (result == null) {
      logger.error { "pass command failed" }
      return null
    }

    if (result.exitCode == 0) {
      return SecretRuntime(lookup.name, result.stdout)
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
      resource: Secret,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<SecretRuntime> {
    val current = lookup(resource.asLookup(), context)
    if (current != null && !resource.tainted) {
      return ApplyResult(current)
    }

    logDebug("creating secret at '${secretName(resource, context)}'", context = log)
    val result =
        runCommand(
            listOf("pass", "insert", "--multiline", "--force", secretName(resource, context)),
            generateSecret(resource.length, resource.allowedChars),
        )

    if (result == null || result.exitCode != 0) {
      logger.error { "pass insert command failed" }
      return ApplyResult(null)
    }

    return ApplyResult(lookup(resource.asLookup(), context))
  }

  override val supportedLookupType = SecretLookup::class

  override val supportedResourceType = Secret::class
}
