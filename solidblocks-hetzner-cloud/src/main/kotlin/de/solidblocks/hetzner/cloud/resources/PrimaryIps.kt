package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.*
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrimaryIpListWrapper(
    @SerialName("primary_ips") val primaryIps: List<PrimaryIpResponse>,
    override val meta: MetaResponse,
) : ListResponse<PrimaryIpResponse> {

  override val list: List<PrimaryIpResponse>
    get() = primaryIps
}

@Serializable
data class PrimaryIpResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
    @SerialName("assignee_id") val assigneeId: Int? = null,
    @SerialName("assignee_type") val assigneeType: String? = null,
) : HetznerDeleteProtectedResource<Long>, HetznerAssignedResource<Long> {

  override val isAssigned: Boolean
    get() = assigneeId != null
}

class HetznerPrimaryIpsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, PrimaryIpResponse>,
    HetznerProtectedResourceApi<Long, PrimaryIpResponse>,
    HetznerAssignedResourceApi {

  override suspend fun listPaged(
      page: Int,
      perPage: Int,
      filter: Map<String, FilterValue>,
      labelSelectors: Map<String, LabelSelectorValue>,
  ): PrimaryIpListWrapper =
      api.get("v1/primary_ips?${listQuery(page, perPage, filter, labelSelectors)}")
          ?: throw RuntimeException("failed list primary ips")

  override suspend fun delete(id: Long) = api.simpleDelete("v1/primary_ips/$id")

  override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post(
          "v1/primary_ips/$id/actions/change_protection",
          ChangeVolumeProtectionRequest(delete),
      ) ?: throw RuntimeException("failed to change primary ip protection")

  override suspend fun action(id: Long): ActionResponseWrapper =
      api.get("v1/primary_ips/actions/$id")
          ?: throw RuntimeException("failed to get primary ip action")

  override suspend fun unassign(id: Long): ActionResponseWrapper? =
      api.post("v1/primary_ips/$id/actions/unassign")
}
