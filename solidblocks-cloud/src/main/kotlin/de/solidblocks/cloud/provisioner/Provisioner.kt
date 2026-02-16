package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.Resource
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.Waiter
import de.solidblocks.cloud.utils.Waiter.Companion.defaultWaiter
import de.solidblocks.ssh.SSHClient
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logWarning
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class Provisioner(
    val provisionersRegistry: ProvisionersRegistry,
    val endpointWaiter: Waiter = defaultWaiter(),
) {

  private val logger = KotlinLogging.logger {}

  suspend fun diff(
      resourceGroups: List<ResourceGroup>,
      context: ProvisionerContext,
      log: LogContext,
  ): Result<Map<ResourceGroup, List<ResourceDiff>>> {
    val resourceGroupDiffs =
        resourceGroups
            .map { resourceGroup ->
              val resourceGroupLogContext = log.indent()

              logInfo(
                  "planning changes for resource group ${resourceGroup.name}",
                  context = resourceGroupLogContext,
              )
              val diffLogContext = resourceGroupLogContext.indent()

              val diffs =
                  when (val result = diff(resourceGroup, context, diffLogContext)) {
                    is Error<List<ResourceDiff>> -> return Error(result.error)
                    is Success<List<ResourceDiff>> -> result.data
                  }

              diffs.forEach {
                logDebug(
                    "diff status for ${it.resource.logText()} is '${it.status}'",
                    context = diffLogContext,
                )
                when (it.status) {
                  unknown ->
                      logInfo("unknown ${it.resource.logText()} TODO", context = diffLogContext)
                  missing ->
                      logInfo("will create ${it.resource.logText()}", context = diffLogContext)
                  up_to_date ->
                      logInfo("${it.resource.logText()} is up-to-date", context = diffLogContext)
                  has_changes -> {
                    if (it.needsRecreate()) {
                      logInfo(
                          """${it.resource.logText()} has breaking changes and needs to be re-created
                            ${
                                            it.changes.joinToString("\n") {
                                                "${it.name} should be '${it.expectedValue}' but was '${it.actualValue}'"
                                            }
                                        }
                                """
                              .trimIndent(),
                          context = diffLogContext,
                      )
                    } else {
                      logInfo(
                          "${it.resource.logText()} has pending changes ${it.changes}",
                          context = diffLogContext,
                      )
                    }
                  }

                  parent_missing ->
                      logInfo(
                          "parent resource for ${it.resource.logText()} is missing",
                          context = diffLogContext,
                      )

                  duplicate -> {
                    return Error(
                        it.duplicateErrorMessage
                            ?: "<unknown duplicate error message for ${it.resource.logText()}>",
                    )
                  }
                }
              }

              resourceGroup to diffs
            }
            .toMap()

    return Success(resourceGroupDiffs)
  }

  private fun diff(
      resourceGroup: ResourceGroup,
      context: ProvisionerContext,
      log: LogContext,
  ): Result<List<ResourceDiff>> = runBlocking {
    logger.info { "creating diff for ${resourceGroup.logText()}" }

    val resources = resourceGroup.hierarchicalResourceList().toSet()
    val result = mutableListOf<ResourceDiff>()

    for (resource in resources) {
      logDebug("creating diff for ${resource.logText()}", context = log)
      try {
        logger.info { "creating diff for ${resource.logText()}" }
        val diff =
            provisionersRegistry.diff<Resource, InfrastructureResourceRuntime>(resource, context)
                ?: return@runBlocking Error("diff failed for ${resource.logText()}")

        result.add(diff)
        logDebug("finished diff for ${resource.logText()}", context = log)
      } catch (e: Exception) {
        logger.error(e) { "diff failed for ${resource.logText()}" }

        val hasMissingParent =
            resource.dependsOn.any { parent ->
              result.any { it.resource == parent && (it.status == missing) }
            }

        if (hasMissingParent) {
          result.add(ResourceDiff(resource, parent_missing))
        } else {
          if (resource.dependsOn.isEmpty()) {
            return@runBlocking Error("diff failed for ${resource.logText()} (${e.message})")
          } else {
            logWarning("diff failed for ${resource.logText()} (${e.message})", context = log)
            result.add(ResourceDiff(resource, parent_missing))
          }
        }
      }
    }

    return@runBlocking Success(result.toList())
  }

  suspend fun apply(
      resources: List<Resource>,
      context: ProvisionerContext,
      log: LogContext,
  ): Result<Unit> {
    val success =
        resources
            .map { resource ->
              val runtime =
                  try {
                    provisionersRegistry.apply<Resource, InfrastructureResourceRuntime>(
                        resource,
                        context,
                        log,
                    )
                  } catch (e: Exception) {
                    logger.error(e) { "creating ${resource.logText()} failed" }
                    null
                  }

              runtime
            }
            .all { it != null }

    return if (success) {
      Success(Unit)
    } else {
      Error("")
    }
  }

  fun apply(
      resourceGroupDiffs: Map<ResourceGroup, List<ResourceDiff>>,
      context: ProvisionerContext,
      log: LogContext,
  ): Result<Unit> {
    return runBlocking {
      resourceGroupDiffs.map { (resourceGroup, diffs) ->
        logger.info { "rolling out changes for ${resourceGroup.logText()}" }

        for (diffToDestroy in diffs.filter { it.needsRecreate() }) {
          val resource = diffToDestroy.resource
          logInfo("destroying ${resource.logText()}", context = log)

          val result = provisionersRegistry.destroy<Resource>(resource, context, log)
          if (!result) {
            return@runBlocking Error("destroying ${resource.logText()} failed")
          }
        }

        val duplicatesDiff = diffs.firstOrNull { it.status == duplicate }
        if (duplicatesDiff != null) {
          return@runBlocking Error<Unit>(
              duplicatesDiff.duplicateErrorMessage ?: "<unknown error message>",
          )
        }

        val resourcesToApply =
            diffs
                .filter {
                  it.status != ResourceDiffStatus.up_to_date &&
                      it.status != ResourceDiffStatus.duplicate
                }
                .map { it.resource }
                .hierarchicalResourceList()

        for (resource in resourcesToApply) {
          logInfo("creating ${resource.logText()}", context = log)

          val runtime =
              try {
                provisionersRegistry.apply<Resource, InfrastructureResourceRuntime>(
                    resource,
                    context,
                    log.indent(),
                )
              } catch (e: Exception) {
                logger.error(e) { "creating ${resource.logText()} failed" }
                null
              }

          if (runtime == null) {
            return@runBlocking Error("creating ${resource.logText()} failed")
          }

          runtime.endpoints().forEach {
            when (it.protocol) {
              EndpointProtocol.ssh -> {
                val sshPortOpen =
                    endpointWaiter.waitForCondition {
                      try {
                        logInfo(
                            "waiting for SSH on endpoint '${it.address}:${it.port}'",
                            context = log,
                        )
                        SSHClient(it.address, context.sshKeyPair).command("whoami").exitCode == 0
                      } catch (e: Exception) {
                        logger.error(e) {
                          "error waiting for '${it.protocol}' endpoint ${it.address}:${it.port}"
                        }
                        false
                      }
                    }

                if (!sshPortOpen) {
                  return@runBlocking Error<Unit>(
                      "error waiting for SSH on ${it.address}:${it.port}",
                  )
                }

                val sshClient = SSHClient(it.address, context.sshKeyPair)

                val cloudInitFinished =
                    endpointWaiter.waitForCondition {
                      try {
                        logInfo(
                            "waiting for cloud-init to finish on '${it.address}:${it.port}'",
                            context = log,
                        )
                        sshClient
                            .command("test -f /var/lib/cloud/instance/boot-finished")
                            .exitCode == 0
                      } catch (e: Exception) {
                        false
                      }
                    }

                if (!cloudInitFinished) {
                  return@runBlocking Error<Unit>(
                      "error waiting for cloud-init to finish on ${it.address}:${it.port}",
                  )
                }

                val result = sshClient.command("cat /var/lib/cloud/data/status.json")
                if (result.exitCode != 0) {
                  return@runBlocking Error<Unit>(
                      "error fetching cloud-init result from ${it.address}:${it.port}",
                  )
                }

                val cloudInitResultHasErrors =
                    try {
                      val json = Json { this.ignoreUnknownKeys = true }

                      val cloudInitResult: CloudInitResultWrapper? =
                          json.decodeFromString(result.stdOut)
                      if (cloudInitResult == null) {
                        return@runBlocking Error<Unit>(
                            "error deserializing cloud-init result from ${it.address}:${it.port}",
                        )
                      }

                      cloudInitResult.hasErrors
                    } catch (e: Exception) {
                      logError("failed to deserialize cloud-init status")
                      false
                    }

                if (cloudInitResultHasErrors) {
                  return@runBlocking Error<Unit>(
                      "cloud-init has errors on ${it.address}:${it.port}",
                  )
                }
              }
            }
          }
        }
      }

      return@runBlocking Success(Unit)
    }
  }
}
