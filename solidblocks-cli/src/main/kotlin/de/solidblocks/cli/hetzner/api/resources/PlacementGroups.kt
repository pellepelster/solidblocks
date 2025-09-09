package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerNamedResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlacementGroupsListWrapper(
    @SerialName("placement_groups") val placementGroups: List<PlacementGroupResponse>,
    override val meta: Meta,
) : ListResponse<PlacementGroupResponse> {

  override val list: List<PlacementGroupResponse>
    get() = placementGroups
}

@Serializable
data class PlacementGroupResponse(override val id: Long, override val name: String) :
    HetznerNamedResource

class HetznerPlacementGroupsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<PlacementGroupResponse> {

  suspend fun listPaged(page: Int = 0, perPage: Int = 25): PlacementGroupsListWrapper =
      api.get("v1/placement_groups?page=$page&per_page=$perPage")

  override suspend fun list() =
      api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

  override suspend fun delete(id: Long) = api.simpleDelete("v1/placement_groups/$id")
}
