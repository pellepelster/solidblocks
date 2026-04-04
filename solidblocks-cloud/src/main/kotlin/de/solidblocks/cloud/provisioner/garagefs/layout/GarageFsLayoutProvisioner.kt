package de.solidblocks.cloud.provisioner.garagefs.layout

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.equalsIgnoreOrder
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.BaseGarageFsProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.ApplyClusterLayoutRequest
import de.solidblocks.garagefs.ClusterLayoutNodeRequest
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.garagefs.UpdateClusterLayoutRequest
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class GarageFsLayoutProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsLayoutLookup, GarageFsLayoutRuntime>,
    InfrastructureResourceProvisioner<GarageFsLayout, GarageFsLayoutRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun diff(resource: GarageFsLayout, context: CloudProvisionerContext) =
      when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<GarageFsLayoutRuntime> -> ResourceDiff(resource, unknown)
        is Success<GarageFsLayoutRuntime> -> {
          context.withApiClients(resource.server, resource.adminToken) { apis ->
            when (apis) {
              is Error<GarageFsApi> -> ResourceDiff(resource, unknown)
              is Success<GarageFsApi> -> {
                val status = apis.data.clusterApi.getClusterStatus()

                if (status.nodes.map { it.id } equalsIgnoreOrder result.data.nodes) {
                  ResourceDiff(resource, up_to_date)
                } else {
                  ResourceDiff(
                      resource,
                      has_changes,
                      changes =
                          listOf(
                              ResourceDiffItem(
                                  "nodes",
                                  expectedValue = status.nodes.joinToString(", ") { it.id },
                                  actualValue =
                                      if (result.data.nodes.isNotEmpty()) {
                                        result.data.nodes.joinToString(", ") { it }
                                      } else {
                                        "<empty>"
                                      },
                              ),
                          ),
                  )
                }
              }
            }
          }
        }
      }

  override suspend fun lookup(lookup: GarageFsLayoutLookup, context: CloudProvisionerContext) =
      when (val result = lookupInternal(lookup, context)) {
        is Error<GarageFsLayoutRuntime> -> null
        is Success<GarageFsLayoutRuntime> -> result.data
      }

  suspend fun lookupInternal(
      lookup: GarageFsLayoutLookup,
      context: CloudProvisionerContext,
  ): Result<GarageFsLayoutRuntime> =
      context.withApiClients(lookup.server, lookup.adminToken) { apis ->
        when (apis) {
          is Error<GarageFsApi> -> Error(apis.error)
          is Success<GarageFsApi> -> {
            val layout = apis.data.clusterLayoutApi.getClusterLayout()
            Success(GarageFsLayoutRuntime(lookup.name, layout.roles!!.map { it.id }))
          }
        }
      }

  override suspend fun apply(
      resource: GarageFsLayout,
      context: CloudProvisionerContext,
      log: LogContext,
  ): Result<GarageFsLayoutRuntime> {
    val runtime = lookup(resource.asLookup(), context)

    context.withApiClients(resource.server, resource.adminToken) {
      val api =
          when (it) {
            is Error<GarageFsApi> -> throw RuntimeException(it.error)
            is Success<GarageFsApi> -> it.data
          }

      val layout = api.clusterLayoutApi.getClusterLayout()
      if ((layout.stagedRoleChanges ?: emptyList()).isNotEmpty()) {
        throw RuntimeException(
            "GarageFs has unexpected pending changes: ${(layout.stagedRoleChanges ?: emptyList()).joinToString(",")}",
        )
      }

      val statusNodeIds = api.clusterApi.getClusterStatus().nodes.map { it.id }
      val layoutNodeIds = (layout.roles ?: emptyList()).map { it.id }

      val nodesToAdd = statusNodeIds.filter { !layoutNodeIds.contains(it) }

      if (nodesToAdd.isNotEmpty()) {
        logger.info {
          "adding nodes ${layoutNodeIds.joinToString(", ")} to layout for ${resource.server.logText()}"
        }

        val request =
            UpdateClusterLayoutRequest(
                roles =
                    nodesToAdd.map {
                      ClusterLayoutNodeRequest(it, resource.capacity, emptyList(), "dc1")
                    },
            )
        val response = api.clusterLayoutApi.updateClusterLayout(request)
        api.clusterLayoutApi.applyClusterLayout(ApplyClusterLayoutRequest(response.version!! + 1))
      } else {
        logger.info { "to layout is up to date for ${resource.server.logText()}" }
      }
    }

    return lookup(resource.asLookup(), context)?.let { Success(it) }
        ?: Error<GarageFsLayoutRuntime>("error creating ${resource.logText()}")
  }

  override val supportedLookupType: KClass<*> = GarageFsLayoutLookup::class

  override val supportedResourceType: KClass<*> = GarageFsLayout::class
}
