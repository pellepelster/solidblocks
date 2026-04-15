package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectedResource
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectionResponse
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class LoadBalancerFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class LoadBalancerNameFilter(name: String) : LoadBalancerFilter("name", name)

@Serializable
data class LoadBalancerCreateResponseWrapper(@SerialName("load_balancer") val loadBalancer: LoadBalancerResponse, @SerialName("action") val action: ActionResponse)

@Serializable
data class LoadBalancersListWrapper(@SerialName("load_balancers") val loadBalancers: List<LoadBalancerResponse>, override val meta: MetaResponse) : ListResponse<LoadBalancerResponse> {
    override val list: List<LoadBalancerResponse>
        get() = loadBalancers
}

@Serializable
data class LoadBalancerResponseWrapper(@SerialName("load_balancer") val loadBalancer: LoadBalancerResponse)

@Suppress("ktlint:standard:enum-entry-name-case")
enum class LoadBalancerType {
    lb11,
    lb21,
    lb31,
}

@Serializable
data class LoadBalancerCreateRequest(@SerialName("load_balancer_type") val loadBalancerType: LoadBalancerType, val name: String, val location: HetznerLocation, val labels: Map<String, String>? = null)

enum class LoadBalancerHealthStatus {
    healthy,
    unhealthy,
    unknown,
}

@Serializable
data class LoadBalancerHealthStatusResponse(@SerialName("listen_port") val listenPort: Int, val status: LoadBalancerHealthStatus)

@Serializable
data class LoadBalancerTargetServerResponse(val id: Long)

@Serializable
data class LoadBalancerTargetsResponse(val server: LoadBalancerTargetServerResponse, @SerialName("health_status") val status: List<LoadBalancerHealthStatusResponse> = emptyList())

@Serializable
data class LoadBalancerTargetResponse(
    val type: LoadBalancerTargetType,
    @SerialName("health_status") val status: List<LoadBalancerHealthStatusResponse> = emptyList(),
    @SerialName("label_selector") val labelSelector: LoadBalancerLabelSelectorResponse? = null,
    val server: LoadBalancerTargetServerResponse? = null,
    val targets: List<LoadBalancerTargetsResponse>? = null,
)

enum class LoadBalancerTargetType {
    server,
    label_selector,
    ip,
}

@Serializable
data class LoadBalancerAttachServerRequest(val id: Long)

@Serializable
data class LoadBalancerAttachRequest(val type: LoadBalancerTargetType, val server: LoadBalancerAttachServerRequest, @SerialName("use_private_ip") val usePrivateIp: Boolean)

@Serializable
data class LoadBalancerRemoveTargetRequest(
    val type: LoadBalancerTargetType,
    val server: LoadBalancerAttachServerRequest? = null,
    @SerialName("label_selector") val labelSelector: LoadBalancerLabelSelectorRequest? = null,
)

@Serializable
data class LoadBalancerLabelSelectorRequest(val selector: String)

@Serializable
data class LoadBalancerLabelSelectorResponse(val selector: String)

enum class LoadBalancerProtocol {
    http,
    https,
    tcp,
}

@Serializable
data class LoadBalancerServiceHttpRequest(
    @SerialName("cookie_name") val cookieName: String? = null,
    @SerialName("cookie_lifetime") val cookieLifetime: Int? = null,
    val certificates: List<Long>? = null,
    @SerialName("redirect_http") val redirectHttp: Boolean? = null,
    @SerialName("sticky_sessions") val stickySessions: Boolean? = null,
)

@Serializable
data class LoadBalancerHealthCheckHttpRequest(
    val domain: String? = null,
    val path: String? = null,
    val response: String? = null,
    @SerialName("status_codes") val statusCodes: List<String>? = null,
    val tls: Boolean? = null,
)

@Serializable
data class LoadBalancerHealthCheckRequest(
    val protocol: LoadBalancerProtocol,
    val port: Int,
    val interval: Int,
    val timeout: Int,
    val retries: Int,
    val http: LoadBalancerHealthCheckHttpRequest? = null,
)

@Serializable
data class LoadBalancerAddServiceRequest(
    val protocol: LoadBalancerProtocol,
    @SerialName("listen_port") val listenPort: Int,
    @SerialName("destination_port") val destinationPort: Int,
    @SerialName("proxyprotocol") val proxyProtocol: Boolean,
    val http: LoadBalancerServiceHttpRequest? = null,
    @SerialName("health_check") val healthCheck: LoadBalancerHealthCheckRequest? = null,
)

@Serializable
data class LoadBalancerUpdateServiceRequest(
    @SerialName("listen_port") val listenPort: Int,
    val protocol: LoadBalancerProtocol? = null,
    @SerialName("destination_port") val destinationPort: Int? = null,
    val proxyprotocol: Boolean? = null,
    val http: LoadBalancerServiceHttpRequest? = null,
    @SerialName("health_check") val healthCheck: LoadBalancerHealthCheckRequest? = null,
)

@Serializable
data class LoadBalancerDeleteServiceRequest(@SerialName("listen_port") val listenPort: Int)

enum class LoadBalancerAlgorithmType {
    round_robin,
    least_connections,
}

@Serializable
data class LoadBalancerChangeAlgorithmRequest(val type: LoadBalancerAlgorithmType)

@Serializable
data class LoadBalancerAttachToNetworkRequest(val network: Long, val ip: String? = null)

@Serializable
data class LoadBalancerDetachFromNetworkRequest(val network: Long)

@Serializable
data class LoadBalancerUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

@Serializable
data class LoadBalancerServiceResponse(@SerialName("health_check") val healthCheck: LoadBalancerHealthCheckResponse)

@Serializable
data class LoadBalancerHealthCheckResponse(val interval: Int, val retries: Int)

@Serializable
data class LoadBalancerResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
    val targets: List<LoadBalancerTargetResponse>,
    @SerialName("private_net") val privateNetworks: List<PrivateNetworkResponse> = emptyList(),
    val services: List<LoadBalancerServiceResponse> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
) : HetznerDeleteProtectedResource<Long>

@Serializable
data class ChangeLoadBalancerProtectionRequest(val delete: Boolean)

class HetznerLoadBalancersApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, LoadBalancerResponse, LoadBalancerFilter>,
    HetznerProtectedResourceApi<Long, LoadBalancerResponse, LoadBalancerFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<LoadBalancerFilter>, labelSelectors: Map<String, LabelSelectorValue>): LoadBalancersListWrapper =
        api.get("v1/load_balancers?${listQuery(page, perPage, filter, labelSelectors)}&sort=name")
            ?: throw RuntimeException("failed to list load balancers")

    suspend fun get(id: Long) = api.get<LoadBalancerResponseWrapper>("v1/load_balancers/$id/")?.loadBalancer

    suspend fun get(name: String) = list(listOf(LoadBalancerNameFilter(name))).singleOrNull()

    override suspend fun delete(id: Long): Boolean = api.delete("v1/load_balancers/$id")

    suspend fun attachServer(id: Long, serverId: Long): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/add_target",
        LoadBalancerAttachRequest(
            LoadBalancerTargetType.server,
            LoadBalancerAttachServerRequest(serverId),
            false,
        ),
    ) ?: throw RuntimeException("failed to attach server '$serverId' to load balancer $id")

    override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/change_protection",
        ChangeLoadBalancerProtectionRequest(delete),
    ) ?: throw RuntimeException("failed to change load balancer '$id' protection")

    override suspend fun action(id: Long): ActionResponseWrapper? = api.get("v1/load_balancers/actions/$id")

    suspend fun actions(id: Long) = api.get<ActionsListResponseWrapper>("v1/load_balancers/$id/actions")
        ?: throw RuntimeException("failed to fetch balancers actions")

    suspend fun create(request: LoadBalancerCreateRequest) = api.post<LoadBalancerCreateResponseWrapper>("v1/load_balancers", request)

    suspend fun update(id: Long, request: LoadBalancerUpdateRequest) = api.put<LoadBalancerResponseWrapper>("v1/load_balancers/$id", request)

    suspend fun removeTarget(id: Long, request: LoadBalancerRemoveTargetRequest): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/remove_target",
        request,
    ) ?: throw RuntimeException("failed to remove target from load balancer '$id'")

    suspend fun addService(id: Long, request: LoadBalancerAddServiceRequest): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/add_service",
        request,
    ) ?: throw RuntimeException("failed to add service to load balancer '$id'")

    suspend fun updateService(id: Long, request: LoadBalancerUpdateServiceRequest): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/update_service",
        request,
    ) ?: throw RuntimeException("failed to update service on load balancer '$id'")

    suspend fun deleteService(id: Long, listenPort: Int): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/delete_service",
        LoadBalancerDeleteServiceRequest(listenPort),
    ) ?: throw RuntimeException("failed to delete service on load balancer '$id'")

    suspend fun changeAlgorithm(id: Long, algorithmType: LoadBalancerAlgorithmType): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/change_algorithm",
        LoadBalancerChangeAlgorithmRequest(algorithmType),
    ) ?: throw RuntimeException("failed to change algorithm for load balancer '$id'")

    suspend fun attachToNetwork(id: Long, request: LoadBalancerAttachToNetworkRequest): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/attach_to_network",
        request,
    ) ?: throw RuntimeException("failed to attach load balancer '$id' to network")

    suspend fun detachFromNetwork(id: Long, networkId: Long): ActionResponseWrapper = api.post(
        "v1/load_balancers/$id/actions/detach_from_network",
        LoadBalancerDetachFromNetworkRequest(networkId),
    ) ?: throw RuntimeException("failed to detach load balancer '$id' from network")

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { api.loadBalancers.action(it) })

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)
}
