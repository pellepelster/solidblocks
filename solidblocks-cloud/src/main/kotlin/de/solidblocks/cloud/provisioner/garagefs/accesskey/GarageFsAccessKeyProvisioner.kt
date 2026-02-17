package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.bucket.BaseGarageFsProvisioner
import de.solidblocks.utils.LogContext
import fr.deuxfleurs.garagehq.model.UpdateKeyRequestBody
import kotlin.reflect.KClass

class GarageFsAccessKeyProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsAccessKeyLookup, GarageFsAccessKeyRuntime>,
    InfrastructureResourceProvisioner<
        GarageFsAccessKey,
        GarageFsAccessKeyRuntime,
    > {

  override suspend fun diff(resource: GarageFsAccessKey, context: ProvisionerContext) =
      lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
          ?: ResourceDiff(resource, missing)

  override suspend fun lookup(lookup: GarageFsAccessKeyLookup, context: ProvisionerContext) =
      context.withApiClients(lookup.server, lookup.adminToken.asLookup()) { apiClients ->
        apiClients
            ?.accessKeyApi
            ?.listKeys()
            ?.firstOrNull { it.name == lookup.name }
            ?.let {
              val keyInfo = apiClients?.accessKeyApi?.getKeyInfo(it.id, showSecretKey = true)

              if (keyInfo?.secretAccessKey == null) {
                return@withApiClients null
              }

              GarageFsAccessKeyRuntime(lookup.name, it.id, keyInfo.secretAccessKey!!)
            }
      }

  override suspend fun apply(
      resource: GarageFsAccessKey,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<GarageFsAccessKeyRuntime> {
    val runtime = lookup(resource.asLookup(), context)
    if (runtime != null) {
      return ApplyResult(runtime)
    }

    context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
      it?.accessKeyApi?.createKey(UpdateKeyRequestBody(name = resource.name))
    }

    return ApplyResult(lookup(resource.asLookup(), context))
  }

  override val supportedLookupType: KClass<*> = GarageFsAccessKeyLookup::class

  override val supportedResourceType: KClass<*> = GarageFsAccessKey::class
}
