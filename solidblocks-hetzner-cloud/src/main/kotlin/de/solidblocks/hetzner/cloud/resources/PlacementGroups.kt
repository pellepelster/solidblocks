package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class PlacementGroupFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class PlacementGroupNameFilter(name: String) : PlacementGroupFilter("name", name)

class PlacementGroupTypeFilter(type: PlacementGroupType) : PlacementGroupFilter("type", type.toString())

enum class PlacementGroupType {
    spread,
}

@Serializable
data class PlacementGroupsListWrapper(@SerialName("placement_groups") val placementGroups: List<PlacementGroupResponse>, override val meta: MetaResponse) : ListResponse<PlacementGroupResponse> {
    override val list: List<PlacementGroupResponse>
        get() = placementGroups
}

@Serializable
data class PlacementGroupResponse(
    override val id: Long,
    override val name: String,
    val type: PlacementGroupType,
    val labels: Map<String, String>? = emptyMap(),
    val servers: List<Long> = emptyList(),
) : HetznerNamedResource<Long>

@Serializable
data class PlacementGroupResponseWrapper(@SerialName("placement_group") val placementGroup: PlacementGroupResponse)

@Serializable
data class PlacementGroupCreateRequest(
    val name: String,
    val type: PlacementGroupType,
    val labels: Map<String, String>? = null,
)

@Serializable
data class PlacementGroupUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

class HetznerPlacementGroupsApi(private val api: HetznerApi) : HetznerDeleteResourceApi<Long, PlacementGroupResponse, PlacementGroupFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<PlacementGroupFilter>, labelSelectors: Map<String, LabelSelectorValue>): PlacementGroupsListWrapper =
        api.get("v1/placement_groups?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list placement groups")

    override suspend fun delete(id: Long) = api.delete("v1/placement_groups/$id")

    suspend fun get(id: Long) = api.get<PlacementGroupResponseWrapper>("v1/placement_groups/$id")?.placementGroup

    suspend fun get(name: String) = list(listOf(PlacementGroupNameFilter(name))).singleOrNull()

    suspend fun create(request: PlacementGroupCreateRequest) = api.post<PlacementGroupResponseWrapper>("v1/placement_groups", request)

    suspend fun update(id: Long, request: PlacementGroupUpdateRequest) = api.put<PlacementGroupResponseWrapper>("v1/placement_groups/$id", request)
}
