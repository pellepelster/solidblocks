package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteWithActionResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.InstantSerializer
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectedResource
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectionResponse
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class PublicNet(@SerialName("enable_ipv4") val enableIpv4: Boolean, @SerialName("enable_ipv6") val enableIpv6: Boolean)

@Serializable
data class ServerCreateRequest(
    val name: String,
    val location: HetznerLocation,
    @SerialName("server_type") val type: HetznerServerType,
    val image: String,
    @SerialName("placement_group") val placementGroup: String? = null,
    @SerialName("ssh_keys") val sshKeys: List<Long>? = null,
    val networks: List<String>? = null,
    val firewall: List<String>? = null,
    val volumes: List<Long>? = null,
    @SerialName("user_data") val userData: String? = null,
    val labels: Map<String, String>? = null,
    @SerialName("public_net") val publicNet: PublicNet? = null,
)

@Serializable
data class ServersListWrapper(val servers: List<ServerResponse>, override val meta: MetaResponse) : ListResponse<ServerResponse> {
    override val list: List<ServerResponse>
        get() = servers
}

@Serializable
data class ServerNetworkAttachRequest(val network: Long, val ip: String? = null, @SerialName("alias_ips") val aliasIps: List<String>? = null, @SerialName("ip_range") val ipRange: String? = null)

@Serializable
data class ServerNetworkDetachRequest(val network: Long)

@Serializable
data class ServerChangeTypeRequest(@SerialName("server_type") val serverType: HetznerServerType, @SerialName("upgrade_disk") val upgradeDisk: Boolean)

@Serializable
data class ServerChangeDnsPtrRequest(val ip: String, @SerialName("dns_ptr") val dnsPtr: String?)

@Serializable
data class ServerAddToPlacementGroupRequest(@SerialName("placement_group") val placementGroup: Long)

@Serializable
data class ChangeServerProtectionRequest(val delete: Boolean, val rebuild: Boolean)

@Serializable
data class ServerResponseWrapper(@SerialName("server") val server: ServerResponse)

@Serializable
data class ServerUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

@Serializable
data class ServerCreateResponseWrapper(@SerialName("server") val server: ServerResponse, @SerialName("action") val action: ActionResponse)

open class ServerFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class ServerNameFilter(name: String) : ServerFilter("name", name)

class ServerStatusFilter(status: ServerStatus) : ServerFilter("status", status.toString())

enum class ServerStatus {
    running,
    initializing,
    starting,
    stopping,
    off,
    deleting,
    migrating,
    rebuilding,
    unknown,
}

@Serializable
data class ServerResponse
@OptIn(ExperimentalTime::class)
constructor(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
    val status: ServerStatus,
    @SerialName("server_type") val type: ServerTypeResponse,
    val image: ImageResponse,
    val volumes: List<Long>,
    val location: LocationResponse,
    val labels: Map<String, String> = emptyMap(),
    @SerialName("private_net") val privateNetwork: List<PrivateNetworkResponse> = emptyList(),
    @SerialName("public_net") val publicNetwork: PublicNetworkResponse? = null,
    @Serializable(with = InstantSerializer::class) val created: Instant,
) : HetznerDeleteProtectedResource<Long>

class HetznerServersApi(private val api: HetznerApi) :
    HetznerDeleteWithActionResourceApi<Long, ServerResponse, ServerFilter>,
    HetznerProtectedResourceApi<Long, ServerResponse, ServerFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<ServerFilter>, labelSelectors: Map<String, LabelSelectorValue>): ServersListWrapper =
        api.get("v1/servers?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list servers")

    override suspend fun delete(id: Long): ActionResponseWrapper = api.deleteWithAction("v1/servers/$id") ?: throw RuntimeException("failed to delete server")

    override suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/servers/actions/$id") ?: throw RuntimeException("failed to get server action")

    override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper = api.post(
        "v1/servers/$id/actions/change_protection",
        ChangeServerProtectionRequest(delete, delete),
    ) ?: throw RuntimeException("failed to change server protection")

    suspend fun attachToNetwork(id: Long, request: ServerNetworkAttachRequest): ActionResponseWrapper = api.post(
        "v1/servers/$id/actions/attach_to_network",
        request,
    ) ?: throw RuntimeException("failed to attach server to network")

    suspend fun actions(id: Long) = api.get<ActionsListResponseWrapper>("v1/servers/$id/actions")
        ?: throw RuntimeException("failed to fetch server actions")

    suspend fun get(id: Long) = api.get<ServerResponseWrapper>("v1/servers/$id")?.server

    suspend fun get(name: String) = list(listOf(ServerNameFilter(name))).singleOrNull()

    suspend fun create(request: ServerCreateRequest): ServerCreateResponseWrapper = api.post<ServerCreateResponseWrapper>("v1/servers", request)

    suspend fun update(id: Long, request: ServerUpdateRequest) = api.put<ServerResponseWrapper>("v1/servers/$id", request)

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { api.servers.action(it) })

    suspend fun shutdown(id: Long): ActionResponseWrapper = api.post("v1/servers/$id/actions/shutdown")
        ?: throw RuntimeException("failed to shutdown server")

    suspend fun powerOn(id: Long): ActionResponseWrapper = api.post("v1/servers/$id/actions/poweron")
        ?: throw RuntimeException("failed to power on server")

    suspend fun powerOff(id: Long): ActionResponseWrapper = api.post("v1/servers/$id/actions/poweroff")
        ?: throw RuntimeException("failed to power off server")

    suspend fun reboot(id: Long): ActionResponseWrapper = api.post("v1/servers/$id/actions/reboot")
        ?: throw RuntimeException("failed to reboot server")

    suspend fun detachFromNetwork(id: Long, networkId: Long): ActionResponseWrapper = api.post(
        "v1/servers/$id/actions/detach_from_network",
        ServerNetworkDetachRequest(networkId),
    ) ?: throw RuntimeException("failed to detach server from network")

    suspend fun reset(id: Long): ActionResponseWrapper = api.post("v1/servers/$id/actions/reset")
        ?: throw RuntimeException("failed to reset server")

    suspend fun changeType(id: Long, request: ServerChangeTypeRequest): ActionResponseWrapper = api.post(
        "v1/servers/$id/actions/change_type",
        request,
    ) ?: throw RuntimeException("failed to change server type")

    suspend fun changeDnsPtr(id: Long, ip: String, dnsPtr: String?): ActionResponseWrapper = api.post(
        "v1/servers/$id/actions/change_dns_ptr",
        ServerChangeDnsPtrRequest(ip, dnsPtr),
    ) ?: throw RuntimeException("failed to change server DNS PTR")

    suspend fun addToPlacementGroup(id: Long, placementGroupId: Long): ActionResponseWrapper = api.post(
        "v1/servers/$id/actions/add_to_placement_group",
        ServerAddToPlacementGroupRequest(placementGroupId),
    ) ?: throw RuntimeException("failed to add server to placement group")

    suspend fun removeFromPlacementGroup(id: Long): ActionResponseWrapper = api.post("v1/servers/$id/actions/remove_from_placement_group")
        ?: throw RuntimeException("failed to remove server from placement group")

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)
}
