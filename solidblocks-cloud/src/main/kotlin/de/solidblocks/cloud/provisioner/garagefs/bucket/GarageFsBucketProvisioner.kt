package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.*
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.BucketAliasRequest
import de.solidblocks.garagefs.CreateBucketRequest
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.garagefs.UpdateBucketRequest
import de.solidblocks.garagefs.UpdateBucketWebsiteAccess
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class GarageFsBucketProvisioner : BaseGarageFsProvisioner(), ResourceLookupProvider<GarageFsBucketLookup, GarageFsBucketRuntime>, InfrastructureResourceProvisioner<GarageFsBucket, GarageFsBucketRuntime> {

    private val logger = KotlinLogging.logger {}

    fun <T> equalsIgnoreOrder(list1: List<T>, list2: List<T>) = list1.size == list2.size && list1.toSet() == list2.toSet()

    override suspend fun diff(resource: GarageFsBucket, context: ProvisionerContext) = when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<GarageFsBucketRuntime?> -> ResourceDiff(resource, unknown)
        is Success<GarageFsBucketRuntime?> -> {

            if (result.data == null) {
                ResourceDiff(resource, missing)
            } else {
                val changes = mutableListOf<ResourceDiffItem>()

                val globalAliasesWithOutOwnName = result.data.globalAliases.filter { it != resource.name }
                if (!equalsIgnoreOrder(resource.websiteAccessDomains, globalAliasesWithOutOwnName)) {
                    changes.add(
                        ResourceDiffItem(
                            "website access domains",
                            true,
                            false,
                            false,
                            resource.websiteAccessDomains,
                            globalAliasesWithOutOwnName,
                        ),
                    )
                }

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

    private suspend fun lookupInternal(lookup: GarageFsBucketLookup, context: ProvisionerContext): Result<GarageFsBucketRuntime?> = context.withApiClients(lookup.server, lookup.adminToken.asLookup()) { apis ->
        when (apis) {
            is Error<GarageFsApi> -> Error(apis.error)
            is Success<GarageFsApi> -> Success(apis.data.bucketApi.listBuckets().firstOrNull { it.globalAliases.contains(lookup.name) }?.let {
                val bucketInfo = apis.data.bucketApi.getBucketInfo(it.id)
                GarageFsBucketRuntime(lookup.name, it.id, bucketInfo.websiteAccess, bucketInfo.globalAliases)
            })
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
                is Error<GarageFsApi> -> throw RuntimeException(it.error)
                is Success<GarageFsApi> -> it.data
            }

            val bucketId = current?.id ?: apis.bucketApi.createBucket(CreateBucketRequest(resource.name)).id

            val globalAliasesWithOutOwnName = current?.globalAliases?.filter { it != resource.name } ?: emptyList()
            val globalAliasesToRemove = globalAliasesWithOutOwnName.filter { !resource.websiteAccessDomains.contains(it) }
            val globalAliasesToAdd = resource.websiteAccessDomains.filter { !globalAliasesWithOutOwnName.contains(it) }

            globalAliasesToRemove.forEach {
                apis.bucketAliasApi.removeBucketAlias(BucketAliasRequest(bucketId = bucketId, globalAlias = it, accessKeyId = "", localAlias = ""))
            }

            globalAliasesToAdd.forEach {
                apis.bucketAliasApi.addBucketAlias(BucketAliasRequest(bucketId = bucketId, globalAlias = it, accessKeyId = "", localAlias = ""))
            }

            apis.bucketApi.updateBucket(
                bucketId,
                UpdateBucketRequest(
                    websiteAccess = UpdateBucketWebsiteAccess(
                        indexDocument = if (resource.websiteAccess) {
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
