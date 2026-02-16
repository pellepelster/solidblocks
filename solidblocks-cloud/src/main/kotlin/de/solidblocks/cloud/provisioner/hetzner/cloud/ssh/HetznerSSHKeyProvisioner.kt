package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.hetzner.cloud.resources.SSHKeysCreateRequest
import de.solidblocks.hetzner.cloud.resources.SSHKeysUpdateRequest
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.MessageDigest
import java.util.*
import kotlin.reflect.KClass

class HetznerSSHKeyProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<SSHKeyLookup, SSHKeyRuntime>,
    InfrastructureResourceProvisioner<SSHKey, SSHKeyRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun lookup(
      lookup: SSHKeyLookup,
      context: ProvisionerContext,
  ) =
      api.sshKeys.get(lookup.name)?.let {
        SSHKeyRuntime(it.id, it.name, it.fingerprint, it.publicKey, it.labels)
      }

  suspend fun listAll() =
      api.sshKeys.list().map {
        SSHKeyRuntime(it.id, it.name, it.fingerprint, it.publicKey, it.labels)
      }

  override suspend fun apply(
      resource: SSHKey,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<SSHKeyRuntime> {
    val runtime = lookup(resource.asLookup(), context)

    val sshKey =
        if (runtime == null) {
          api.sshKeys.create(
              SSHKeysCreateRequest(
                  resource.name,
                  resource.publicKey,
                  resource.labels,
              ),
          )
          lookup(resource.asLookup(), context)
        } else {
          runtime
        }

    if (sshKey == null) {
      return ApplyResult(null)
    }

    api.sshKeys.update(sshKey.id, SSHKeysUpdateRequest(resource.name, resource.labels))

    return ApplyResult(lookup(resource.asLookup(), context))
  }

  override suspend fun diff(
      resource: SSHKey,
      context: ProvisionerContext,
  ): ResourceDiff? {
    val runtime = lookup(resource.asLookup(), context)

    if (runtime == null) {
      val fingerprint = computeFingerprint(resource.publicKey)
      val duplicateKey = listAll().firstOrNull { it.fingerprint == fingerprint }

      return if (duplicateKey == null) {
        ResourceDiff(resource, missing)
      } else {
        ResourceDiff(
            resource,
            duplicate,
            duplicateErrorMessage =
                "another key with the fingerprint '${duplicateKey.fingerprint}' already exists (${duplicateKey.name})",
        )
      }
    }

    val changes = createLabelDiff(resource, runtime)
    return if (changes.isEmpty()) {
      ResourceDiff(resource, up_to_date)
    } else {
      ResourceDiff(resource, has_changes, changes = changes)
    }
  }

  override suspend fun destroy(
      resource: SSHKey,
      context: ProvisionerContext,
      logContext: LogContext,
  ) = lookup(resource.asLookup(), context)?.let { api.sshKeys.delete(it.id) } ?: false

  private fun computeFingerprint(publicKey: String): String {
    val parts = publicKey.trim().split(Regex("\\s+"))
    if (parts.size < 2) {
      throw IllegalArgumentException("Invalid SSH public key format")
    }

    val base64Key = parts[1]
    val keyBytes = Base64.getDecoder().decode(base64Key)

    val md5 = MessageDigest.getInstance("MD5")
    val digest = md5.digest(keyBytes)

    return digest.joinToString(":") { "%02x".format(it) }
  }

  override val supportedLookupType: KClass<*> = SSHKeyLookup::class

  override val supportedResourceType: KClass<*> = SSHKey::class
}
