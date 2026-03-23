package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class NetworkType {
    cloud,
    vswitch,
}

enum class NetworkZone {
    `eu-central`,
}

@Serializable
data class NetworkUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

@Serializable
data class NetworkCreateSubnetRequest(
    val type: NetworkType,
    @SerialName("ip_range") val ipRange: String,
    @SerialName("network_zone") val networkZone: String,
    @SerialName("vswitch_id") val vswitchId: String,
)

@Serializable
data class NetworkCreateRouteRequest(val destination: String, val gateway: String)

@Serializable
data class NetworkCreateRequest(
    val name: String,
    @SerialName("ip_range") val ipRange: String,
    val labels: Map<String, String>? = null,
    @SerialName("public_net") val publicNet: PublicNet? = null,
    @SerialName("expose_routes_to_vswitch") val exposeRoutesToVswitch: Boolean = false,
    val routes: List<NetworkCreateRouteRequest> = emptyList(),
)

@Serializable
data class NetworksSubnetCreateRequest(
    val type: NetworkType,
    @SerialName("ip_range") val ipRange: String,
    @SerialName("network_zone") val networkZone: NetworkZone,
)

@Serializable
data class NetworksListResponseWrapper(
    val networks: List<NetworkResponse>,
    override val meta: MetaResponse,
) : ListResponse<NetworkResponse> {

    override val list: List<NetworkResponse>
        get() = networks
}

@Serializable
data class ChangeNetworkProtectionRequest(val delete: Boolean)

@Serializable
data class NetworkResponseWrapper(val network: NetworkResponse)

@Serializable
data class PrivateNetworkResponse(val network: Long, val ip: String)

@Serializable
data class PublicNetworkIPResponse(val ip: String)

@Serializable
data class PublicNetworkResponse(val ipv4: PublicNetworkIPResponse?)

@Serializable
data class NetworkResponse(
    override val id: Long,
    override val name: String,
    @SerialName("ip_range") val ipRange: String,
    override val protection: HetznerDeleteProtectionResponse,
    val labels: Map<String, String>,
    val subnets: List<NetworkSubnetResponse>,
) : HetznerDeleteProtectedResource<Long>

@Serializable
data class NetworkSubnetResponse(
    val type: NetworkType,
    @SerialName("ip_range") val ipRange: String,
    @SerialName("network_zone") val networkZone: NetworkZone,
    val gateway: String,
)

class HetznerNetworksApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, NetworkResponse>,
    HetznerProtectedResourceApi<Long, NetworkResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>,
    ): NetworksListResponseWrapper =
        api.get("v1/networks?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list networks")

    override suspend fun delete(id: Long) = api.delete("v1/networks/$id")

    suspend fun get(id: Long) = api.get<NetworkResponseWrapper>("v1/networks/$id")?.network

    suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

    suspend fun create(request: NetworkCreateRequest) =
        api.post<NetworkResponseWrapper>("v1/networks", request)

    suspend fun addSubnet(network: Long, request: NetworksSubnetCreateRequest) =
        api.post<ActionResponseWrapper>("v1/networks/$network/actions/add_subnet", request)

    override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post("v1/networks/$id/actions/change_protection", ChangeNetworkProtectionRequest(delete))
            ?: throw RuntimeException("failed to change network protection")

    suspend fun waitForAction(
        action: ActionResponseWrapper,
        logCallback: ((String) -> Unit)? = null,
    ) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) =
        waitForAction(action.id, logCallback)

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) =
        api.waitForAction(id, logCallback, { api.networks.action(it) })

    suspend fun update(id: Long, request: NetworkUpdateRequest) =
        api.put<NetworkResponseWrapper>("v1/networks/$id", request)

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/networks/actions/$id") ?: throw RuntimeException("failed to get network action")
}
