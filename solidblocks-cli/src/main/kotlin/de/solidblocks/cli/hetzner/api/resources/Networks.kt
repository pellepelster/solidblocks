package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerProtectedResourceApi
import de.solidblocks.cli.hetzner.api.listQuery
import de.solidblocks.cli.hetzner.api.model.*
import kotlinx.serialization.Serializable

@Serializable
data class NetworksListResponseWrapper(val networks: List<NetworkResponse>, override val meta: Meta) :
    ListResponse<NetworkResponse> {

    override val list: List<NetworkResponse>
        get() = networks
}

@Serializable
data class NetworkResponseWrapper(val network: NetworkResponse)

@Serializable
data class PrivateNetworkResponse(val network: Long, val ip: String)

@Serializable
data class NetworkResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
) : HetznerProtectedResource

class HetznerNetworksApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<NetworkResponse>, HetznerProtectedResourceApi<NetworkResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): NetworksListResponseWrapper =
        api.get("v1/networks?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list networks")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/networks/$id")

    suspend fun get(id: Long) = api.get<NetworkResponseWrapper>("v1/networks/$id")?.network

    suspend fun get(name: String) =
        list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post("v1/networks/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete))
            ?: throw RuntimeException("failed to change network protection")

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/networks/actions/$id") ?: throw RuntimeException("failed to get network action")
}
