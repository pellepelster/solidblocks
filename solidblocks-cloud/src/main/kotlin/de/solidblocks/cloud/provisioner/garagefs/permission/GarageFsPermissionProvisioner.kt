package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.bucket.BaseGarageFsProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.BucketKeyPermChangeRequest
import de.solidblocks.garagefs.BucketKeyPermRequest
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class GarageFsPermissionProvisioner : BaseGarageFsProvisioner(), ResourceLookupProvider<GarageFsPermissionLookup, GarageFsPermissionRuntime>,
    InfrastructureResourceProvisioner<GarageFsPermission, GarageFsPermissionRuntime> {

    private val logger = KotlinLogging.logger {}

    suspend fun lookupInternal(lookup: GarageFsPermissionLookup, context: ProvisionerContext): Result<GarageFsPermissionRuntime?> = context.withApiClients(lookup.server, lookup.adminToken) { apis ->
        when (apis) {
            is Error<GarageFsApi> -> Error(apis.error)
            is Success<GarageFsApi> -> {
                val bucket = context.lookup(lookup.bucket) ?: return@withApiClients Success(null)
                val accessKey = context.lookup(lookup.accessKey) ?: return@withApiClients Success(null)
                val permission = apis.data.accessKeyApi.getKeyInfo(accessKey.id).buckets.singleOrNull {
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
                }.let { Success(it) }
            }
        }
    }


    override suspend fun lookup(
        lookup: GarageFsPermissionLookup,
        context: ProvisionerContext,
    ) = when (val result = lookupInternal(lookup, context)) {
        is Error<GarageFsPermissionRuntime?> -> null
        is Success<GarageFsPermissionRuntime?> -> result.data
    }

    override suspend fun apply(
        resource: GarageFsPermission,
        context: ProvisionerContext,
        log: LogContext,
    ): Result<GarageFsPermissionRuntime> {
        val bucket = context.lookup(resource.bucket.asLookup()) ?: return Error<GarageFsPermissionRuntime>("${resource.bucket.logText()} not found")
        val accessKey = context.lookup(resource.accessKey.asLookup()) ?: return Error<GarageFsPermissionRuntime>("${resource.accessKey.logText()} not found")

        context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
            val apis = when (it) {
                is Error<GarageFsApi> -> return@withApiClients Error<GarageFsPermissionRuntime>(it.error)
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

        return lookup(resource.asLookup(), context)?.let {
            Success(it)
        } ?: Error<GarageFsPermissionRuntime>("error creating ${resource.logText()}")
    }

    override suspend fun diff(resource: GarageFsPermission, context: ProvisionerContext) = when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<GarageFsPermissionRuntime?> -> ResourceDiff(resource, unknown)
        is Success<GarageFsPermissionRuntime?> -> {
            if (result.data == null) {
                ResourceDiff(resource, missing)
            } else {
                val changes = mutableListOf<ResourceDiffItem>()

                if (result.data.owner != resource.owner) {
                    changes.add(
                        ResourceDiffItem(
                            "owner",
                            true,
                            false,
                            false,
                            resource.owner,
                            result.data.owner,
                        ),
                    )
                }

                if (result.data.read != resource.read) {
                    changes.add(
                        ResourceDiffItem(
                            "read",
                            true,
                            false,
                            false,
                            resource.read,
                            result.data.read,
                        ),
                    )
                }

                if (result.data.write != resource.write) {
                    changes.add(
                        ResourceDiffItem(
                            "owner",
                            true,
                            false,
                            false,
                            resource.write,
                            result.data.write,
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
    }

    override val supportedLookupType: KClass<*> = GarageFsPermissionLookup::class

    override val supportedResourceType: KClass<*> = GarageFsPermission::class
}
