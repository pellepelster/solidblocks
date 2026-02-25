package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.*
import de.solidblocks.hetzner.cloud.model.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicNet(
    @SerialName("enable_ipv4") val enableIpv4: Boolean,
    @SerialName("enable_ipv6") val enableIpv6: Boolean,
)

@Serializable
data class ServerCreateRequest(
    val name: String,
    val location: String,
    @SerialName("server_type") val type: String,
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
data class ServersListWrapper(val servers: List<ServerResponse>, override val meta: MetaResponse) :
    ListResponse<ServerResponse> {
  override val list: List<ServerResponse>
    get() = servers
}

@Serializable data class ServerResponseWrapper(@SerialName("server") val server: ServerResponse)

@Serializable
data class ServerUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

@Serializable
data class ServerCreateResponseWrapper(
    @SerialName("server") val server: ServerResponse,
    @SerialName("action") val action: ActionResponse,
)

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
    HetznerDeleteWithActionResourceApi<Long, ServerResponse>,
    HetznerProtectedResourceApi<Long, ServerResponse> {

  override suspend fun listPaged(
      page: Int,
      perPage: Int,
      filter: Map<String, FilterValue>,
      labelSelectors: Map<String, LabelSelectorValue>,
  ): ServersListWrapper =
      api.get("v1/servers?${listQuery(page, perPage, filter, labelSelectors)}")
          ?: throw RuntimeException("failed to list servers")

  override suspend fun delete(id: Long): ActionResponseWrapper =
      api.complexDelete("v1/servers/$id") ?: throw RuntimeException("failed to delete server")

  override suspend fun action(id: Long): ActionResponseWrapper =
      api.get("v1/servers/actions/$id") ?: throw RuntimeException("failed to get server action")

  override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post(
          "v1/servers/$id/actions/change_protection",
          ChangeVolumeProtectionRequest(delete, delete),
      ) ?: throw RuntimeException("failed to change server protection")

  suspend fun actions(id: Long) =
      api.get<ActionsListResponseWrapper>("v1/servers/$id/actions")
          ?: throw RuntimeException("failed to fetch server actions")

  suspend fun get(id: Long) = api.get<ServerResponseWrapper>("v1/servers/$id")?.server

  suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

  suspend fun create(request: ServerCreateRequest) =
      api.post<ServerCreateResponseWrapper>("v1/servers", request)

  suspend fun update(id: Long, request: ServerUpdateRequest) =
      api.put<ServerResponseWrapper>("v1/servers/$id", request)

  suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) =
      api.waitForAction(id, logCallback, { api.servers.action(it) })

  suspend fun shutdown(id: Long): ActionResponseWrapper =
      api.post("v1/servers/$id/actions/shutdown")
          ?: throw RuntimeException("failed to shutdown server")

  suspend fun waitForAction(
      action: ActionResponseWrapper,
      logCallback: ((String) -> Unit)? = null,
  ) = waitForAction(action.action, logCallback)

  suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) =
      waitForAction(action.id, logCallback)
}
