package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.HetznerApi
import de.solidblocks.cli.hetzner.HetznerSimpleResourceApi
import de.solidblocks.cli.hetzner.HetznerNamedResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoadBalancersListWrapper(
    @SerialName("load_balancers") val loadBalancers: List<LoadBalancerResponse>,
    override val meta: Meta
) :
    ListResponse<LoadBalancerResponse> {

    override val list: List<LoadBalancerResponse>
        get() = loadBalancers
}

@Serializable
data class LoadBalancerResponse(override val id: Long, override val name: String) : HetznerNamedResource


class HetznerLoadBalancersApi(private val api: HetznerApi) : HetznerSimpleResourceApi<LoadBalancerResponse> {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): LoadBalancersListWrapper =
        api.get("v1/load_balancers?page=${page}&per_page=${perPage}")

    override suspend fun list() = api.handlePaginatedList { page, perPage ->
        listPaged(page, perPage)
    }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/load_balancers/${id}")

}


