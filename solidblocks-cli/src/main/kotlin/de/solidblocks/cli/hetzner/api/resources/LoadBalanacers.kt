package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
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
data class LoadBalancerResponseWrapper(
    @SerialName("load_balancer")
    val loadbalancer: LoadBalancerResponse,
)

enum class LoadBalancerHealthStatus { healthy, unhealthy, unknown }

@Serializable
data class LoadBalancerHealthStatusResponse(
    @SerialName("listen_port")
    val listenPort: Int,
    val status: LoadBalancerHealthStatus,
)

@Serializable
data class LoadBalancerTargetServerResponse(
    val id: Long,
)

@Serializable
data class LoadBalancerTargetsResponse(
    val server: LoadBalancerTargetServerResponse,
    @SerialName("health_status")
    val status: List<LoadBalancerHealthStatusResponse> = emptyList(),
)

@Serializable
data class LoadBalancerTargetResponse(
    val type: LoadBalancerTargetType,
    @SerialName("health_status")
    val status: List<LoadBalancerHealthStatusResponse> = emptyList(),
    @SerialName("label_selector")
    val labelSelector: LoadBalancerLabelSelectorResponseResponse? = null,
    val server: LoadBalancerTargetServerResponse? = null,
    val targets: List<LoadBalancerTargetsResponse>? = null,
)

enum class LoadBalancerTargetType {
    server, label_selector, ip
}

@Serializable
data class LoadBalancerLabelSelectorResponseResponse(val selector: String)

@Serializable
data class LoadBalancerResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    val targets: List<LoadBalancerTargetResponse>
) : HetznerProtectedResource

@Serializable
data class ChangeLoadBalancerProtectionRequest(val delete: Boolean)

class HetznerLoadBalancersApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<LoadBalancerResponse>, HetznerProtectedResourceApi {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): LoadBalancersListWrapper =
        api.get("v1/load_balancers?page=$page&per_page=$perPage&sort=name")
            ?: throw RuntimeException("failed to list load balancers")

    override suspend fun list() =
        api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

    suspend fun get(id: Long) =
        api.get<LoadBalancerResponseWrapper>("v1/load_balancers/$id/")

    suspend fun get(name: String) = list().firstOrNull { it.name == name }?.let {
        get(it.id)
    }

    override suspend fun delete(id: Long): Boolean = api.simpleDelete("v1/load_balancers/$id")

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post(
            "v1/load_balancers/$id/actions/change_protection",
            ChangeLoadBalancerProtectionRequest(delete),
        ) ?: throw RuntimeException("failed to get load balancers protection")

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/load_balancers/actions/$id") ?: throw RuntimeException("failed to get load balancers action")
}
