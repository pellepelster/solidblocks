package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.FilterValue
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectedResource
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectionResponse
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SubnetType {
  cloud,
  vswitch,
}

@Serializable
data class NetworkCreateSubnetRequest(
    val type: SubnetType,
    @SerialName("ip_range") val ipRange: String,
    @SerialName("network_zone") val networkZone: String,
    @SerialName("vswitch_id") val vswitchId: String,
)

@Serializable
data class NetworkCreateRouteRequest(
    val destination: String,
    val gateway: String,
)

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
data class NetworksListResponseWrapper(
    val networks: List<NetworkResponse>,
    override val meta: MetaResponse,
) : ListResponse<NetworkResponse> {

  override val list: List<NetworkResponse>
    get() = networks
}

@Serializable data class NetworkResponseWrapper(val network: NetworkResponse)

@Serializable data class PrivateNetworkResponse(val network: Long, val ip: String)

@Serializable data class PublicNetworkIPResponse(val ip: String)

@Serializable data class PublicNetworkResponse(val ipv4: PublicNetworkIPResponse?)

@Serializable
data class NetworkResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
) : HetznerDeleteProtectedResource<Long>

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

  override suspend fun delete(id: Long) = api.simpleDelete("v1/networks/$id")

  suspend fun get(id: Long) = api.get<NetworkResponseWrapper>("v1/networks/$id")?.network

  suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

  suspend fun create(request: NetworkCreateRequest) =
      api.post<NetworkResponseWrapper>("v1/networks", request)

  override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post("v1/networks/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete))
          ?: throw RuntimeException("failed to change network protection")

  override suspend fun action(id: Long): ActionResponseWrapper =
      api.get("v1/networks/actions/$id") ?: throw RuntimeException("failed to get network action")
}
