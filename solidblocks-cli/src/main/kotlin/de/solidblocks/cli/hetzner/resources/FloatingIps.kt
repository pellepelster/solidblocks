package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.*
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
    HetznerDeleteResourceApi<FloatingIpResponse>, HetznerProtectedResourceApi {

  suspend fun listPaged(page: Int = 0, perPage: Int = 25): FloatingIPsListWrapper =
      api.get("v1/floating_ips?page=$page&per_page=$perPage")

  override suspend fun list() =
      api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

  override suspend fun delete(id: Long) = api.simpleDelete("v1/floating_ips/$id")

  override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post(
          "v1/floating_ips/$id/actions/change_protection",
          ChangeVolumeProtectionRequest(delete),
      )

  suspend fun unassign(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post("v1/floating_ips/$id/actions/unassign")

  override suspend fun action(id: Long): ActionResponseWrapper =
      api.get("v1/floating_ips/actions/$id")
}
