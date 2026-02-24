package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import fr.deuxfleurs.garagehq.model.CreateBucketRequest
import fr.deuxfleurs.garagehq.model.UpdateBucketRequestBody
import fr.deuxfleurs.garagehq.model.UpdateBucketWebsiteAccess
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class GarageFsBucketProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsBucketLookup, GarageFsBucketRuntime>,
    InfrastructureResourceProvisioner<GarageFsBucket, GarageFsBucketRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: GarageFsBucket, context: ProvisionerContext) = when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<GarageFsBucketRuntime?> -> ResourceDiff(resource, unknown)
        is Success<GarageFsBucketRuntime?> -> {

            if (result.data == null) {
                ResourceDiff(resource, missing)
            } else {
                val changes = mutableListOf<ResourceDiffItem>()
                if (resource.websiteAccess != result.data.websiteAccess) {
                    changes.add(
                        ResourceDiffItem(
                            "public access",
                            true,
                            false,
                            false,
                            resource.websiteAccess,
                            result.data.websiteAccess,
                        ),
                    )
                }

                if (changes.isEmpty()) {
                    ResourceDiff(resource, up_to_date)
                } else {
                    ResourceDiff(resource, has_changes, changes = changes)
                }
            }
        }
    }

    override suspend fun lookup(lookup: GarageFsBucketLookup, context: ProvisionerContext) = when (val result = lookupInternal(lookup, context)) {
        is Error<GarageFsBucketRuntime?> -> null
        is Success<GarageFsBucketRuntime?> -> result.data
    }

    private suspend fun lookupInternal(lookup: GarageFsBucketLookup, context: ProvisionerContext): Result<GarageFsBucketRuntime?> =
        context.withApiClients(lookup.server, lookup.adminToken.asLookup()) { apis ->
            when (apis) {
                is Error<ApiClients> -> Error(apis.error)
                is Success<ApiClients> -> apis.data.bucketApi.listBuckets()
                    .firstOrNull { it.globalAliases.contains(lookup.name) }
                    ?.let {
                        val websiteAccess =
                            apis.data.bucketApi.getBucketInfo(it.id).websiteAccess
                        Success(GarageFsBucketRuntime(lookup.name, it.id, websiteAccess))
                    } ?: Success(null)
            }
        }

    override suspend fun apply(
        resource: GarageFsBucket,
        context: ProvisionerContext,
        log: LogContext,
    ): ApplyResult<GarageFsBucketRuntime> {
        val current = lookup(resource.asLookup(), context)

        context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
            val apis = when (it) {
                is Error<ApiClients> -> throw RuntimeException(it.error)
                is Success<ApiClients> -> it.data
            }

            val id =
                if (current == null) {
                    apis.bucketApi.createBucket(CreateBucketRequest(resource.name)).id
                } else {
                    current.id
                }

            apis.bucketApi.updateBucket(
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
