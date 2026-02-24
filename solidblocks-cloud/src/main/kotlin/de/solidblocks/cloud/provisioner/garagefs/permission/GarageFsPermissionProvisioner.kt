package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.bucket.BaseGarageFsProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import fr.deuxfleurs.garagehq.model.ApiBucketKeyPerm
import fr.deuxfleurs.garagehq.model.BucketKeyPermChangeRequest
import kotlin.reflect.KClass

class GarageFsPermissionProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsPermissionLookup, GarageFsPermissionRuntime>,
    InfrastructureResourceProvisioner<
            GarageFsPermission,
            GarageFsPermissionRuntime,
            > {
    override suspend fun lookup(
        lookup: GarageFsPermissionLookup,
        context: ProvisionerContext,
    ): GarageFsPermissionRuntime? {
        val bucket = context.lookup(lookup.bucket) ?: return null
        val accessKey = context.lookup(lookup.accessKey) ?: return null

        return context.withApiClients(lookup.server, lookup.adminToken) {
            val apis = when (it) {
                is Error<ApiClients> -> throw RuntimeException(it.error)
                is Success<ApiClients> -> it.data
            }

            val permission =
                apis.accessKeyApi.getKeyInfo(accessKey.id).buckets?.singleOrNull {
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
                null
            }
        }
    }

    override suspend fun apply(
        resource: GarageFsPermission,
        context: ProvisionerContext,
        log: LogContext,
    ): ApplyResult<GarageFsPermissionRuntime> {
        val bucket = context.lookup(resource.bucket.asLookup()) ?: return ApplyResult(null)
        val accessKey = context.lookup(resource.accessKey.asLookup()) ?: return ApplyResult(null)

        context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
            val apis = when (it) {
                is Error<ApiClients> -> throw RuntimeException(it.error)
                is Success<ApiClients> -> it.data
            }

            apis.permissionApi.allowBucketKey(
                BucketKeyPermChangeRequest(
                    accessKey.id,
                    bucket.id,
                    ApiBucketKeyPerm(resource.owner, resource.read, resource.write),
                ),
            )
            apis.permissionApi.denyBucketKey(
                BucketKeyPermChangeRequest(
                    accessKey.id,
                    bucket.id,
                    ApiBucketKeyPerm(!resource.owner, !resource.read, !resource.write),
                ),
            )
        }

        return ApplyResult(lookup(resource.asLookup(), context))
    }

    override suspend fun diff(resource: GarageFsPermission, context: ProvisionerContext) =
        lookup(resource.asLookup(), context)?.let {
            val changes = mutableListOf<ResourceDiffItem>()

            if (it.owner != resource.owner) {
                changes.add(
                    ResourceDiffItem(
                        "owner",
                        true,
                        false,
                        false,
                        resource.owner,
                        it.owner,
                    ),
                )
            }

            if (it.read != resource.read) {
                changes.add(
                    ResourceDiffItem(
                        "read",
                        true,
                        false,
                        false,
                        resource.read,
                        it.read,
                    ),
                )
            }

            if (it.write != resource.write) {
                changes.add(
                    ResourceDiffItem(
                        "owner",
                        true,
                        false,
                        false,
                        resource.write,
                        it.write,
                    ),
                )
            }

            if (changes.isNotEmpty()) {
                ResourceDiff(resource, has_changes, changes = changes)
            } else {
                ResourceDiff(resource, up_to_date)
            }
        } ?: ResourceDiff(resource, missing)

    override val supportedLookupType: KClass<*> = GarageFsPermissionLookup::class

    override val supportedResourceType: KClass<*> = GarageFsPermission::class
}
