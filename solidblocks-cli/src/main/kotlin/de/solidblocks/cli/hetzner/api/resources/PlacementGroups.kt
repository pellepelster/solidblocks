package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
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

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): PlacementGroupsListWrapper =
        api.get("v1/placement_groups?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list placement groups")

    override suspend fun list(filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>) =
        api.handlePaginatedList(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors
            )
        }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/placement_groups/$id")
}
