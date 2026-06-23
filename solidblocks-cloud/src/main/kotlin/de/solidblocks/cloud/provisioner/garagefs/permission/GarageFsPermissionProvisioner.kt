package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.api.ResourceProvisioner
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffItem
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.ProvisionerLookupContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.BaseGarageFsProvisioner
import de.solidblocks.garagefs.BucketKeyPermChangeRequest
import de.solidblocks.garagefs.BucketKeyPermRequest
import de.solidblocks.garagefs.GarageFsApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class GarageFsPermissionProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsPermissionLookup, GarageFsPermissionRuntime>,
    ResourceProvisioner<GarageFsPermission, GarageFsPermissionRuntime, GarageFsPermissionLookup> {

    private val logger = KotlinLogging.logger {}

    suspend fun lookupInternal(lookup: GarageFsPermissionLookup, context: SSHProvisionerContext): Result<GarageFsPermissionRuntime?> = context.withApiClients(lookup.server, lookup.adminToken) { apis ->
        when (apis) {
            is Error<GarageFsApi> -> Error(apis.error)
            is Success<GarageFsApi> -> {
                val bucket = context.lookup(lookup.bucket) ?: return@withApiClients Success(null)
                val accessKey = context.lookup(lookup.accessKey) ?: return@withApiClients Success(null)
                val permission =
                    apis.data.accessKeyApi.getKeyInfo(accessKey.id).buckets.singleOrNull {
                        it.globalAliases.contains(bucket.name)
                    }

                if (permission != null) {
                    GarageFsPermissionRuntime(
                        bucket,
                        accessKey,
                        permission.permissions.owner!!,
                        permission.permissions.read!!,
                        permission.permissions.write!!,
                    )
                } else {
                    GarageFsPermissionRuntime(bucket, accessKey, false, false, false)
                }
                    .let { Success(it) }
            }
        }
    }

    override suspend fun lookup(lookup: GarageFsPermissionLookup, context: ProvisionerLookupContext) = when (val result = lookupInternal(lookup, context as SSHProvisionerContext)) {
        is Error<GarageFsPermissionRuntime?> -> null
        is Success<GarageFsPermissionRuntime?> -> result.data
    }

    override suspend fun apply(resource: GarageFsPermission, context: ProvisionerApplyContext): Result<GarageFsPermissionRuntime> {
        val bucket =
            context.lookup(resource.bucket.asLookup())
                ?: return Error<GarageFsPermissionRuntime>("${resource.bucket.logText()} not found")
        val accessKey =
            context.lookup(resource.accessKey.asLookup())
                ?: return Error<GarageFsPermissionRuntime>("${resource.accessKey.logText()} not found")

        context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
            val apis =
                when (it) {
                    is Error<GarageFsApi> ->
                        return@withApiClients Error<GarageFsPermissionRuntime>(it.error)

                    is Success<GarageFsApi> -> it.data
                }

            apis.permissionApi.allowBucketKey(
                BucketKeyPermChangeRequest(
                    accessKey.id,
                    bucket.id,
                    BucketKeyPermRequest(resource.owner, resource.read, resource.write),
                ),
            )
            apis.permissionApi.denyBucketKey(
                BucketKeyPermChangeRequest(
                    accessKey.id,
                    bucket.id,
                    BucketKeyPermRequest(!resource.owner, !resource.read, !resource.write),
                ),
            )
        }

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<GarageFsPermissionRuntime>("error creating ${resource.logText()}")
    }

    override suspend fun diff(resource: GarageFsPermission, context: ProvisionerDiffContext): Result<ResourceDiff> = Success(
        when (val result = lookupInternal(resource.asLookup(), context)) {
            is Error<GarageFsPermissionRuntime?> -> ResourceDiff(resource, unknown)
            is Success<GarageFsPermissionRuntime?> -> {
                val runtime = result.data
                if (runtime == null) {
                    ResourceDiff(resource, missing)
                } else {
                    val changes = mutableListOf<ResourceDiffItem>()

                    if (runtime.owner != resource.owner) {
                        changes.add(
                            ResourceDiffItem(
                                "owner",
                                true,
                                false,
                                false,
                                resource.owner,
                                runtime.owner,
                            ),
                        )
                    }

                    if (runtime.read != resource.read) {
                        changes.add(
                            ResourceDiffItem(
                                "read",
                                true,
                                false,
                                false,
                                resource.read,
                                runtime.read,
                            ),
                        )
                    }

                    if (runtime.write != resource.write) {
                        changes.add(
                            ResourceDiffItem(
                                "owner",
                                true,
                                false,
                                false,
                                resource.write,
                                runtime.write,
                            ),
                        )
                    }

                    if (changes.isNotEmpty()) {
                        ResourceDiff(resource, has_changes, changes = changes)
                    } else {
                        ResourceDiff(resource, up_to_date)
                    }
                }
            }
        },
    )

    override val supportedLookupType: KClass<*> = GarageFsPermissionLookup::class

    override val supportedResourceType: KClass<*> = GarageFsPermission::class
}
