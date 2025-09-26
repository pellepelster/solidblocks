package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import de.solidblocks.cli.hetzner.api.model.FilterValue
import de.solidblocks.cli.hetzner.api.model.HetznerProtectedResource
import de.solidblocks.cli.hetzner.api.model.HetznerProtectionResponse
import de.solidblocks.cli.hetzner.api.model.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.model.ListResponse
import de.solidblocks.cli.hetzner.api.model.Meta
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

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): FloatingIPsListWrapper =
        api.get("v1/floating_ips?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list floating ips")

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
