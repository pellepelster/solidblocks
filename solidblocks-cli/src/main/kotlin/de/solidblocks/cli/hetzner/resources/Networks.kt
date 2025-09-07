package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.*
import kotlinx.serialization.Serializable

@Serializable
data class NetworksListWrapper(val networks: List<NetworkResponse>, override val meta: Meta) :
    ListResponse<NetworkResponse> {

  override val list: List<NetworkResponse>
    get() = networks
}

@Serializable
data class NetworkResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
) : HetznerProtectedResource

class HetznerNetworksApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<NetworkResponse>, HetznerProtectedResourceApi {

  suspend fun listPaged(page: Int = 0, perPage: Int = 25): NetworksListWrapper =
      api.get("v1/networks?page=$page&per_page=$perPage")

  override suspend fun list() =
      api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

  override suspend fun delete(id: Long) = api.simpleDelete("v1/networks/$id")

  override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post("v1/networks/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete))

  override suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/networks/actions/$id")
}
