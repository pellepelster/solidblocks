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

open class FloatingIpFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class FloatingIpNameFilter(name: String) : FloatingIpFilter("name", name)

enum class FloatingIpType {
    ipv4,
    ipv6,
}

@Serializable
data class FloatingIpsListWrapper(@SerialName("floating_ips") val floatingIps: List<FloatingIpResponse>, override val meta: MetaResponse) : ListResponse<FloatingIpResponse> {
    override val list: List<FloatingIpResponse>
        get() = floatingIps
}

@Serializable
data class FloatingIpResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
    val ip: String,
    val type: FloatingIpType,
    @SerialName("assignee_id") val assigneeId: Long? = null,
    @SerialName("assignee_type") val assigneeType: String? = null,
    val labels: Map<String, String> = emptyMap(),
    val description: String? = null,
) : HetznerDeleteProtectedResource<Long>

@Serializable
data class FloatingIpResponseWrapper(@SerialName("floating_ip") val floatingIp: FloatingIpResponse)

@Serializable
data class FloatingIpCreateRequest(
    val name: String,
    val type: FloatingIpType,
    @SerialName("home_location") val homeLocation: HetznerLocation? = null,
    val server: Long? = null,
    val description: String? = null,
    val labels: Map<String, String>? = null,
)

@Serializable
data class FloatingIpCreateResponseWrapper(@SerialName("floating_ip") val floatingIp: FloatingIpResponse, val action: ActionResponse? = null)

@Serializable
data class FloatingIpUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null, val description: String? = null)

@Serializable
data class FloatingIpAssignRequest(val server: Long)

@Serializable
data class FloatingIpChangeDnsPtrRequest(val ip: String, @SerialName("dns_ptr") val dnsPtr: String?)

@Serializable
data class ChangeFloatingIpProtectionRequest(val delete: Boolean)

class HetznerFloatingIpsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, FloatingIpResponse, FloatingIpFilter>,
    HetznerProtectedResourceApi<Long, FloatingIpResponse, FloatingIpFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<FloatingIpFilter>, labelSelectors: Map<String, LabelSelectorValue>): FloatingIpsListWrapper =
        api.get("v1/floating_ips?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list floating ips")

    override suspend fun delete(id: Long) = api.delete("v1/floating_ips/$id")

    override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper = api.post(
        "v1/floating_ips/$id/actions/change_protection",
        ChangeFloatingIpProtectionRequest(delete),
    ) ?: throw RuntimeException("failed to change floating ip protection")

    override suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/floating_ips/actions/$id")
        ?: throw RuntimeException("failed to get floating ip action '$id'")

    suspend fun get(id: Long) = api.get<FloatingIpResponseWrapper>("v1/floating_ips/$id")?.floatingIp

    suspend fun get(name: String) = list(listOf(FloatingIpNameFilter(name))).singleOrNull()

    suspend fun create(request: FloatingIpCreateRequest) = api.post<FloatingIpCreateResponseWrapper>("v1/floating_ips", request)

    suspend fun update(id: Long, request: FloatingIpUpdateRequest) = api.put<FloatingIpResponseWrapper>("v1/floating_ips/$id", request)

    suspend fun assign(id: Long, serverId: Long): ActionResponseWrapper = api.post(
        "v1/floating_ips/$id/actions/assign",
        FloatingIpAssignRequest(serverId),
    ) ?: throw RuntimeException("failed to assign floating ip '$id' to server '$serverId'")

    suspend fun unassign(id: Long): ActionResponseWrapper? = api.post("v1/floating_ips/$id/actions/unassign")

    suspend fun changeDnsPtr(id: Long, ip: String, dnsPtr: String?): ActionResponseWrapper = api.post(
        "v1/floating_ips/$id/actions/change_dns_ptr",
        FloatingIpChangeDnsPtrRequest(ip, dnsPtr),
    ) ?: throw RuntimeException("failed to change DNS PTR for floating ip '$id'")

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { api.floatingIps.action(it) })

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)
}
