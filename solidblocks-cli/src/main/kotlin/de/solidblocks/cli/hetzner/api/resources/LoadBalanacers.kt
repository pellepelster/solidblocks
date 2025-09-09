package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerProtectedResource
import de.solidblocks.cli.hetzner.api.HetznerProtectedResourceApi
import de.solidblocks.cli.hetzner.api.HetznerProtectionResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoadBalancersListWrapper(
    @SerialName("load_balancers") val loadBalancers: List<LoadBalancerResponse>,
    override val meta: Meta,
) : ListResponse<LoadBalancerResponse> {

  override val list: List<LoadBalancerResponse>
    get() = loadBalancers
}

@Serializable
data class LoadBalancerResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
) : HetznerProtectedResource

@Serializable data class ChangeLoadBalancerProtectionRequest(val delete: Boolean)

class HetznerLoadBalancersApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<LoadBalancerResponse>, HetznerProtectedResourceApi {

  suspend fun listPaged(page: Int = 0, perPage: Int = 25): LoadBalancersListWrapper =
      api.get("v1/load_balancers?page=$page&per_page=$perPage")

  override suspend fun list() =
      api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

  override suspend fun delete(id: Long): Boolean = api.simpleDelete("v1/load_balancers/$id")

  override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post(
          "v1/load_balancers/$id/actions/change_protection",
          ChangeLoadBalancerProtectionRequest(delete),
      )

  override suspend fun action(id: Long): ActionResponseWrapper =
      api.get("v1/load_balancers/actions/$id")
}
