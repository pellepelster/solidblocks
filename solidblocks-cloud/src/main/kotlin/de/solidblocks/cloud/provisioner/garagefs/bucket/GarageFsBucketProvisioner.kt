package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.utils.LogContext
import fr.deuxfleurs.garagehq.model.CreateBucketRequest
import fr.deuxfleurs.garagehq.model.UpdateBucketRequestBody
import fr.deuxfleurs.garagehq.model.UpdateBucketWebsiteAccess
import kotlin.reflect.KClass

class GarageFsBucketProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsBucketLookup, GarageFsBucketRuntime>,
    InfrastructureResourceProvisioner<
        GarageFsBucket,
        GarageFsBucketRuntime,
    > {

  override suspend fun diff(resource: GarageFsBucket, context: ProvisionerContext) =
      lookup(resource.asLookup(), context)?.let {
        val changes = mutableListOf<ResourceDiffItem>()
        if (resource.websiteAccess != it.websiteAccess) {
          changes.add(
              ResourceDiffItem(
                  "public access",
                  true,
                  false,
                  false,
                  resource.websiteAccess,
                  it.websiteAccess,
              ),
          )
        }

        if (changes.isEmpty()) {
          ResourceDiff(resource, up_to_date)
        } else {
          ResourceDiff(resource, has_changes, changes = changes)
        }
      } ?: ResourceDiff(resource, missing)

  override suspend fun lookup(lookup: GarageFsBucketLookup, context: ProvisionerContext) =
      context.withApiClients(lookup.server, lookup.adminToken.asLookup()) { apis ->
        apis
            ?.bucketApi
            ?.listBuckets()
            ?.firstOrNull { it.globalAliases.contains(lookup.name) }
            ?.let {
              val websiteAccess =
                  apis.bucketApi.getBucketInfo(it.id).websiteAccess ?: return@withApiClients null
              GarageFsBucketRuntime(lookup.name, it.id, websiteAccess)
            }
      }

  override suspend fun apply(
      resource: GarageFsBucket,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<GarageFsBucketRuntime> {
    val current = lookup(resource.asLookup(), context)

    context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
      val id =
          if (current == null) {
            it?.bucketApi?.createBucket(CreateBucketRequest(resource.name))!!.id
          } else {
            current.id
          }

      it?.bucketApi?.updateBucket(
          id,
          UpdateBucketRequestBody(
              websiteAccess =
                  UpdateBucketWebsiteAccess(
                      indexDocument =
                          if (resource.websiteAccess) {
                            "index.html"
                          } else {
                            null
                          },
                      enabled = resource.websiteAccess,
                  ),
          ),
      )
    }

    return ApplyResult(lookup(resource.asLookup(), context))
  }

  override val supportedLookupType: KClass<*> = GarageFsBucketLookup::class

  override val supportedResourceType: KClass<*> = GarageFsBucket::class
}
