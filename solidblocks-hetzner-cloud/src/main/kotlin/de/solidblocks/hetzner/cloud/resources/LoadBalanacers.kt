package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoadBalancersListWrapper(
    @SerialName("load_balancers") val loadBalancers: List<LoadBalancerResponse>,
    override val meta: MetaResponse,
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
    val labelSelector: LoadBalancerLabelSelectorResponse? = null,
    val server: LoadBalancerTargetServerResponse? = null,
    val targets: List<LoadBalancerTargetsResponse>? = null,
)

enum class LoadBalancerTargetType {
    server, label_selector, ip
}

@Serializable
data class LoadBalancerAttachServerRequest(
    val id: Long,
)

@Serializable
data class LoadBalancerAttachRequest(
    val type: LoadBalancerTargetType,
    val server: LoadBalancerAttachServerRequest,
)

@Serializable
data class LoadBalancerLabelSelectorResponse(val selector: String)

@Serializable
data class LoadBalancerServiceResponse(@SerialName("health_check") val healthCheck: LoadBalancerHealthCheckResponse)

@Serializable
data class LoadBalancerHealthCheckResponse(val interval: Int, val retries: Int)

@Serializable
data class LoadBalancerResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    val targets: List<LoadBalancerTargetResponse>,
    @SerialName("private_net")
    val privateNetworks: List<PrivateNetworkResponse> = emptyList(),
    val services: List<LoadBalancerServiceResponse> = emptyList()
) : HetznerProtectedResource

@Serializable
data class ChangeLoadBalancerProtectionRequest(val delete: Boolean)

class HetznerLoadBalancersApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<LoadBalancerResponse>, HetznerProtectedResourceApi<LoadBalancerResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): LoadBalancersListWrapper =
        api.get("v1/load_balancers?${listQuery(page, perPage, filter, labelSelectors)}&sort=name")
            ?: throw RuntimeException("failed to list load balancers")

    suspend fun get(id: Long) =
        api.get<LoadBalancerResponseWrapper>("v1/load_balancers/$id/")?.loadbalancer

    suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

    override suspend fun delete(id: Long): Boolean = api.simpleDelete("v1/load_balancers/$id")

    suspend fun attachServer(id: Long, serverId: Long): ActionResponseWrapper =
        api.post(
            "v1/load_balancers/$id/actions/add_target",
            LoadBalancerAttachRequest(LoadBalancerTargetType.server, LoadBalancerAttachServerRequest(serverId)),
        ) ?: throw RuntimeException("failed to attach server '${serverId}' to load balancer ${id}")

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post(
            "v1/load_balancers/$id/actions/change_protection",
            ChangeLoadBalancerProtectionRequest(delete),
        ) ?: throw RuntimeException("failed to get load balancers protection")

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/load_balancers/actions/$id") ?: throw RuntimeException("failed to get load balancers action")

    suspend fun actions(id: Long) = api.get<ActionsListResponseWrapper>("v1/load_balancers/$id/actions")
        ?: throw RuntimeException("failed to fetch balancers actions")

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, {
        api.loadBalancers.action(it)
    })

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) =
        waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) =
        waitForAction(action.id, logCallback)
}
