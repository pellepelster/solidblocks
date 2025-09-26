package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import de.solidblocks.cli.hetzner.api.model.FilterValue
import de.solidblocks.cli.hetzner.api.model.HetznerNamedResource
import de.solidblocks.cli.hetzner.api.model.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.model.ListResponse
import de.solidblocks.cli.hetzner.api.model.Meta
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

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): PlacementGroupsListWrapper =
        api.get("v1/placement_groups?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list placement groups")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/placement_groups/$id")
}
