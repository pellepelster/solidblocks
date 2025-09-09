package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerAssignedResource
import de.solidblocks.cli.hetzner.api.HetznerAssignedResourceApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerProtectedResource
import de.solidblocks.cli.hetzner.api.HetznerProtectedResourceApi
import de.solidblocks.cli.hetzner.api.HetznerProtectionResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrimaryIpListWrapper(
    @SerialName("primary_ips") val primaryIps: List<PrimaryIpResponse>,
    override val meta: Meta,
) : ListResponse<PrimaryIpResponse> {

  override val list: List<PrimaryIpResponse>
    get() = primaryIps
}

@Serializable
data class PrimaryIpResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    @SerialName("assignee_id") val assigneeId: Int? = null,
    @SerialName("assignee_type") val assigneeType: String? = null,
) : HetznerProtectedResource, HetznerAssignedResource {

  override val isAssigned: Boolean
    get() = assigneeId != null
}

class HetznerPrimaryIpsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<PrimaryIpResponse>,
    HetznerProtectedResourceApi,
    HetznerAssignedResourceApi {

  suspend fun listPaged(page: Int = 0, perPage: Int = 25): PrimaryIpListWrapper =
      api.get("v1/primary_ips?page=$page&per_page=$perPage")

  override suspend fun list() =
      api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

  override suspend fun delete(id: Long) = api.simpleDelete("v1/primary_ips/$id")

  override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post(
          "v1/primary_ips/$id/actions/change_protection",
          ChangeVolumeProtectionRequest(delete),
      )

  override suspend fun action(id: Long): ActionResponseWrapper =
      api.get("v1/primary_ips/actions/$id")

  override suspend fun unassign(id: Long): ActionResponseWrapper =
      api.post("v1/primary_ips/$id/actions/unassign")
}
