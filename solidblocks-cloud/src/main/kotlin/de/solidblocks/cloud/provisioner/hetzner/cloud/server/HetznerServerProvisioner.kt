package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.Constants.sshKeysLabel
import de.solidblocks.cloud.Constants.userDataLabel
import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.utils.HetznerLabels
import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import de.solidblocks.utils.logInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerServerProvisioner(val hcloudToken: String) :
    ResourceLookupProvider<HetznerServerLookup, HetznerServerRuntime>,
    InfrastructureResourceProvisioner<HetznerServer, HetznerServerRuntime> {

  private val logger = KotlinLogging.logger {}

  val api = HetznerApi(hcloudToken)

  override suspend fun lookup(lookup: HetznerServerLookup, context: ProvisionerContext) =
      api.servers.get(lookup.name)?.let {
        HetznerServerRuntime(
            it.id,
            it.name,
            it.status,
            it.image.name ?: "unknown",
            it.type.name,
            it.labels,
            it.volumes.map { volume -> volume.toString() },
            it.privateNetwork.firstOrNull()?.ip,
            it.publicNetwork?.ipv4?.ip,
            it.publicNetwork?.ipv4?.ip?.let { listOf(Endpoint(it, 22, EndpointProtocol.ssh)) }
                ?: emptyList(),
        )
      }

  override suspend fun apply(
      resource: HetznerServer,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<HetznerServerRuntime> {
    var server = lookup(resource.asLookup(), context)

    if (server == null) {
      logger.info { "server '${resource.name}' not found, creating" }

      val sshKeys =
          resource.sshKeys.map {
            val key = context.lookup(it)
            if (key == null) {
              throw RuntimeException("failed to lookup ${it.logText()}")
            }
            key
          }

      val volumes =
          resource.volumes.map {
            val volume = context.lookup(it)
            if (volume == null) {
              throw RuntimeException("failed to resolve ${it.logText()}")
            }

            if (volume.server != null) {
              throw RuntimeException(
                  "volume ${it.logText()} already attached to server ${volume.server}",
              )
            }

            volume
          }

      val userData = context.ensureLookup(resource.userData)

      val labels = HetznerLabels()
      labels.addHashedLabel(sshKeysLabel, sshKeys.joinToString { it.fingerprint })
      labels.addHashedLabel(userDataLabel, userData.userData)

      logDebug(
          "using ssh key(s): ${
                    sshKeys.let {
                        if (it.isEmpty()) {
                            "<none>"
                        } else {
                            it.joinToString(", ") { "${it.name} (${it.id})" }
                        }
                    }
                }",
          context = log,
      )
      logDebug(
          "using volume(s): ${
                    volumes.let {
                        if (it.isEmpty()) {
                            "<none>"
                        } else {
                            it.joinToString(", ") { "${it.id} (${it.id})" }
                        }
                    }
                }",
          context = log,
      )
      logger.info {
        "creating server '${resource.name}' with ssh keys ${sshKeys.joinToString(", ") { "${it.name} (${it.id})" }}"
      }
      val request =
          ServerCreateRequest(
              resource.name,
              resource.location,
              resource.type,
              image = resource.image,
              userData = userData.userData,
              sshKeys = sshKeys.map { it.id },
              volumes = volumes.map { it.id },
              labels = labels.rawLabels(),
          )
      val createRequest = api.servers.create(request)

      if (createRequest == null) {
        logger.error { "failed to create server '${resource.name}'" }
        return ApplyResult(null)
      }

      if (
          !api.servers.waitForAction(createRequest.action) {
            logInfo("waiting for creation of ${resource.logText()}", context = log)
          }
      ) {
        logger.error { "failed to wait for creation of '${resource.name}'" }
        return ApplyResult(null)
      }

      /*
      val result = createServer(resource, context)
      if (!result) {
          logger.info { "creating server '${resource.name}' failed" }
          return false
      }
       */

      server = lookup(resource.asLookup(), context)
    }

    if (server == null) {
      logger.error { "server creation failed" }
      return ApplyResult(null)
    }

    return lookup(resource.asLookup(), context).let {
      logDebug("${resource.logText()} has ip ${it?.publicIpv4 ?: "<none>"}", context = log)
      ApplyResult(it)
    }
  }

  override suspend fun diff(resource: HetznerServer, context: ProvisionerContext): ResourceDiff? {
    val runtime = lookup(resource.asLookup(), context)

    return if (runtime != null) {
      val changes = mutableListOf<ResourceDiffItem>()

      val labels = HetznerLabels(runtime.labels)

      val sshKeys =
          resource.sshKeys.map {
            context.lookup(it) ?: throw RuntimeException("failed to lookup ${it.logText()}")
          }

      val sshKeysHash =
          labels.hashLabelMatches(sshKeysLabel, sshKeys.joinToString { it.fingerprint })

      if (!sshKeysHash.matches) {
        changes.add(
            ResourceDiffItem(
                resource::sshKeys,
                triggersRecreate = true,
                changed = true,
                expectedValue = sshKeysHash.expectedValue,
                actualValue = sshKeysHash.actualValue,
            ),
        )
      }

      val userData = context.lookup(resource.userData) ?: return ResourceDiff(resource, up_to_date)
      val userDataHash =
          labels.hashLabelMatches(
              userDataLabel,
              userData.userData,
          )

      if (!userDataHash.matches) {
        changes.add(
            ResourceDiffItem(
                "user data checksum",
                triggersRecreate = true,
                changed = true,
                expectedValue = userDataHash.expectedValue,
                actualValue = userDataHash.actualValue,
            ),
        )
      }

      if (resource.type != runtime.type) {
        changes.add(ResourceDiffItem("type", true, true, false, resource.type, runtime.type))
      }

      if (resource.image != runtime.image) {
        changes.add(ResourceDiffItem("image", true, true, false, resource.image, runtime.image))
      }

      if (changes.isEmpty()) {
        ResourceDiff(resource, up_to_date)
      } else {
        ResourceDiff(resource, has_changes, changes = changes)
      }
    } else {
      ResourceDiff(resource, missing)
    }
  }

  override suspend fun destroy(
      resource: HetznerServer,
      context: ProvisionerContext,
      logContext: LogContext,
  ) =
      lookup(resource.asLookup(), context)?.let {
        val delete = api.servers.delete(it.id)
        api.servers.waitForAction(delete) {
          logInfo("waiting for deletion of ${resource.logText()}", context = logContext)
        }
      } ?: false

  override val supportedLookupType: KClass<*> = HetznerServerLookup::class

  override val supportedResourceType: KClass<*> = HetznerServer::class
}
