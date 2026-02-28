package de.solidblocks.cloud.provisioner.garagefs.layout

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.bucket.BaseGarageFsProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.ApplyClusterLayoutRequest
import de.solidblocks.garagefs.ClusterLayoutNodeRequest
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.garagefs.UpdateClusterLayoutRequest
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

class GarageFsLayoutProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsLayoutLookup, GarageFsLayoutRuntime>,
    InfrastructureResourceProvisioner<GarageFsLayout, GarageFsLayoutRuntime> {

    override suspend fun diff(resource: GarageFsLayout, context: ProvisionerContext) = when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<GarageFsLayoutRuntime?> -> ResourceDiff(resource, unknown)
        is Success<GarageFsLayoutRuntime?> -> {
            if (result.data == null) {
                ResourceDiff(resource, missing)
            } else {
                ResourceDiff(resource, up_to_date)
            }
        }
    }

    override suspend fun lookup(lookup: GarageFsLayoutLookup, context: ProvisionerContext) =
        when (val result = lookupInternal(lookup, context)) {
            is Error<GarageFsLayoutRuntime?> -> null
            is Success<GarageFsLayoutRuntime?> -> result.data
        }

    suspend fun lookupInternal(lookup: GarageFsLayoutLookup, context: ProvisionerContext): Result<GarageFsLayoutRuntime?> =
        context.withApiClients(lookup.server, lookup.adminToken.asLookup()) { apis ->
            when (apis) {
                is Error<GarageFsApi> -> Error(apis.error)
                is Success<GarageFsApi> -> {
                    val layout = apis.data.clusterLayoutApi.getClusterLayout()

                    if (layout.nodes == null) {
                        Success(null)
                    } else {
                        Success(GarageFsLayoutRuntime(lookup.name, layout.nodes!!.map { it.id }))
                    }
                }
            }

        }

    override suspend fun apply(
        resource: GarageFsLayout,
        context: ProvisionerContext,
        log: LogContext,
    ): ApplyResult<GarageFsLayoutRuntime> {
        val runtime = lookup(resource.asLookup(), context)
        if (runtime != null) {
            return ApplyResult(runtime)
        }

        context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
            val api = when (it) {
                is Error<GarageFsApi> -> throw RuntimeException(it.error)
                is Success<GarageFsApi> -> it.data
            }

            /*
            TODO
            if (layout.stagedRoleChanges?.isNotEmpty()) {
                api.revertClusterLayout()
            }*/

            val status = api.clusterApi.getClusterStatus()

            val request = UpdateClusterLayoutRequest(
                roles = status.nodes.map {
                    ClusterLayoutNodeRequest(it.id, resource.capacity, emptyList(), "dc1")
                }
            )

            val response = api.clusterLayoutApi.updateClusterLayout(request)
            api.clusterLayoutApi.applyClusterLayout(ApplyClusterLayoutRequest(response.version!! + 1))
        }

        return ApplyResult(lookup(resource.asLookup(), context))
    }

    override val supportedLookupType: KClass<*> = GarageFsLayoutLookup::class

    override val supportedResourceType: KClass<*> = GarageFsLayout::class
}
