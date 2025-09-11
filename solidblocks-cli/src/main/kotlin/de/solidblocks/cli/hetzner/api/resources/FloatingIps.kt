package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FloatingIPsListWrapper(
    @SerialName("floating_ips") val floatingIPs: List<FloatingIpResponse>,
    override val meta: Meta,
) : ListResponse<FloatingIpResponse> {

    override val list: List<FloatingIpResponse>
        get() = floatingIPs
}

@Serializable
data class FloatingIpResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    @SerialName("assignee_id") val assigneeId: Int? = null,
    @SerialName("assignee_type") val assigneeType: String? = null,
) : HetznerProtectedResource

class HetznerFloatingIpsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<FloatingIpResponse>, HetznerProtectedResourceApi<FloatingIpResponse> {

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): FloatingIPsListWrapper =
        api.get("v1/floating_ips?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list floating ips")

    override suspend fun list(filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>) =
        api.handlePaginatedList(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors
            )
        }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/floating_ips/$id")

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post(
            "v1/floating_ips/$id/actions/change_protection",
            ChangeVolumeProtectionRequest(delete),
        ) ?: throw RuntimeException("failed to change floating ip protection")

    suspend fun unassign(id: Long, delete: Boolean): ActionResponseWrapper? =
        api.post("v1/floating_ips/$id/actions/unassign")

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/floating_ips/actions/$id") ?: throw RuntimeException("failed to get action for floating")
}
