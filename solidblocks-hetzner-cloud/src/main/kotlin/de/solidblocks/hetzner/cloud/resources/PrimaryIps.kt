package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerAssignedResourceApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.InstantSerializer
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerAssignedResource
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectedResource
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectionResponse
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class PrimaryIpFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class PrimaryIpNameFilter(name: String) : PrimaryIpFilter("name", name)

class PrimaryIpIpFilter(ip: String) : PrimaryIpFilter("ip", ip)

enum class PrimaryIpType {
    ipv4,
    ipv6,
}

enum class PrimaryIpAssigneeType {
    server,
}

@Serializable
data class PrimaryIpsListWrapper(@SerialName("primary_ips") val primaryIps: List<PrimaryIpResponse>, override val meta: MetaResponse) : ListResponse<PrimaryIpResponse> {
    override val list: List<PrimaryIpResponse>
        get() = primaryIps
}

@Serializable
data class PrimaryIpResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
    val ip: String,
    val type: PrimaryIpType,
    @SerialName("assignee_id") val assigneeId: Long? = null,
    @SerialName("assignee_type") val assigneeType: String? = null,
    @SerialName("auto_delete") val autoDelete: Boolean = false,
    val labels: Map<String, String> = emptyMap(),
) : HetznerDeleteProtectedResource<Long>,
    HetznerAssignedResource<Long> {
    override val isAssigned: Boolean
        get() = assigneeId != null
}

@Serializable
data class PrimaryIpResponseWrapper(@SerialName("primary_ip") val primaryIp: PrimaryIpResponse)

@Serializable
data class PrimaryIpCreateRequest(
    val name: String,
    val type: PrimaryIpType,
    @SerialName("assignee_type") val assigneeType: PrimaryIpAssigneeType,
    @SerialName("assignee_id") val assigneeId: Long? = null,
    val datacenter: String? = null,
    @SerialName("auto_delete") val autoDelete: Boolean = false,
    val labels: Map<String, String>? = null,
)

@Serializable
data class PrimaryIpCreateResponseWrapper(@SerialName("primary_ip") val primaryIp: PrimaryIpResponse, val action: ActionResponse? = null)

@Serializable
data class PrimaryIpUpdateRequest(
    val name: String? = null,
    val labels: Map<String, String>? = null,
    @SerialName("auto_delete") val autoDelete: Boolean? = null,
)

@Serializable
data class PrimaryIpAssignRequest(
    @SerialName("assignee_id") val assigneeId: Long,
    @SerialName("assignee_type") val assigneeType: PrimaryIpAssigneeType,
)

@Serializable
data class PrimaryIpChangeDnsPtrRequest(val ip: String, @SerialName("dns_ptr") val dnsPtr: String?)

@Serializable
data class ChangePrimaryIpProtectionRequest(val delete: Boolean)

class HetznerPrimaryIpsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, PrimaryIpResponse, PrimaryIpFilter>,
    HetznerProtectedResourceApi<Long, PrimaryIpResponse, PrimaryIpFilter>,
    HetznerAssignedResourceApi {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<PrimaryIpFilter>, labelSelectors: Map<String, LabelSelectorValue>): PrimaryIpsListWrapper =
        api.get("v1/primary_ips?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list primary ips")

    override suspend fun delete(id: Long) = api.delete("v1/primary_ips/$id")

    override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper = api.post(
        "v1/primary_ips/$id/actions/change_protection",
        ChangePrimaryIpProtectionRequest(delete),
    ) ?: throw RuntimeException("failed to change primary ip protection")

    override suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/primary_ips/actions/$id")
        ?: throw RuntimeException("failed to get primary ip action '$id'")

    override suspend fun unassign(id: Long): ActionResponseWrapper? = api.post("v1/primary_ips/$id/actions/unassign")

    suspend fun get(id: Long) = api.get<PrimaryIpResponseWrapper>("v1/primary_ips/$id")?.primaryIp

    suspend fun get(name: String) = list(listOf(PrimaryIpNameFilter(name))).singleOrNull()

    suspend fun create(request: PrimaryIpCreateRequest) = api.post<PrimaryIpCreateResponseWrapper>("v1/primary_ips", request)

    suspend fun update(id: Long, request: PrimaryIpUpdateRequest) = api.put<PrimaryIpResponseWrapper>("v1/primary_ips/$id", request)

    suspend fun assign(id: Long, assigneeId: Long, assigneeType: PrimaryIpAssigneeType = PrimaryIpAssigneeType.server): ActionResponseWrapper = api.post(
        "v1/primary_ips/$id/actions/assign",
        PrimaryIpAssignRequest(assigneeId, assigneeType),
    ) ?: throw RuntimeException("failed to assign primary ip '$id' to server '$assigneeId'")

    suspend fun changeDnsPtr(id: Long, ip: String, dnsPtr: String?): ActionResponseWrapper = api.post(
        "v1/primary_ips/$id/actions/change_dns_ptr",
        PrimaryIpChangeDnsPtrRequest(ip, dnsPtr),
    ) ?: throw RuntimeException("failed to change DNS PTR for primary ip '$id'")

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { api.primaryIps.action(it) })

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)
}
