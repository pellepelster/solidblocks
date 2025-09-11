package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.InstantSerializer
import de.solidblocks.cli.hetzner.api.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class ServersListWrapper(val servers: List<ServerResponse>, override val meta: Meta) :
    ListResponse<ServerResponse> {
    override val list: List<ServerResponse>
        get() = servers
}

@Serializable
data class ServerResponseWrapper(
    @SerialName("server") val server: ServerResponse,
    val action: ActionResponse? = null
)

@Serializable
data class ServerResponse @OptIn(ExperimentalTime::class) constructor(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    val labels: Map<String, String> = emptyMap(),
    @SerialName("private_net") val privateNetwork: List<PrivateNetworkResponse> = emptyList(),
    @Serializable(with = InstantSerializer::class)
    val created: Instant
) : HetznerProtectedResource

class HetznerServersApi(private val api: HetznerApi) : HetznerDeleteWithActionResourceApi<ServerResponse>,
    HetznerProtectedResourceApi<ServerResponse> {

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue> = emptyMap()
    ): ServersListWrapper {
        return api.get("v1/servers?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list servers")
    }

    override suspend fun delete(id: Long): ActionResponseWrapper =
        api.complexDelete("v1/servers/$id") ?: throw RuntimeException("failed to delete server")

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/servers/actions/$id") ?: throw RuntimeException("failed to get server action")

    override suspend fun list(filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>) =
        api.handlePaginatedList(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors
            )
        }

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper = api.post(
        "v1/servers/$id/actions/change_protection",
        ChangeVolumeProtectionRequest(delete, delete),
    ) ?: throw RuntimeException("failed to change server protection")

    suspend fun actions(id: Long) = api.get<ActionsListResponseWrapper>("v1/servers/$id/actions")
        ?: throw RuntimeException("failed to fetch server actions")

    suspend fun get(id: Long) = api.get<ServerResponseWrapper>("v1/servers/$id")

    suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()?.let {
        get(it.id)
    }

    suspend fun create(request: ServerCreateRequest) = api.post<ServerResponseWrapper>("v1/servers", request)

    suspend fun waitForAction(id: Long) = api.waitForAction(id, {
        api.servers.action(it)
    })

    suspend fun waitForAction(action: ActionResponseWrapper) = waitForAction(action.action)

    suspend fun waitForAction(action: ActionResponse) = waitForAction(action.id)

}
