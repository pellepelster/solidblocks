package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServersListWrapper(val servers: List<ServerResponse>, override val meta: Meta) :
    ListResponse<ServerResponse> {
    override val list: List<ServerResponse>
        get() = servers
}

@Serializable
data class ServerResponseWrapper(
    @SerialName("server")
    val server: ServerResponse,
)

@Serializable
data class ServerResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    val labels: Map<String, String> = emptyMap(),
) : HetznerProtectedResource

class HetznerServersApi(private val api: HetznerApi) :
    HetznerDeleteWithActionResourceApi<ServerResponse>, HetznerProtectedResourceApi {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): ServersListWrapper =
        api.get("v1/servers?page=$page&per_page=$perPage") ?: throw RuntimeException("failed to list servers")

    override suspend fun delete(id: Long): ActionResponseWrapper = api.complexDelete("v1/servers/$id")
        ?: throw RuntimeException("failed to delete server")

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/servers/actions/$id") ?: throw RuntimeException("failed to get server action")

    override suspend fun list() =
        api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post(
            "v1/servers/$id/actions/change_protection",
            ChangeVolumeProtectionRequest(delete, delete),
        ) ?: throw RuntimeException("failed to change server protection")

    suspend fun get(id: Long) =
        api.get<ServerResponseWrapper>("v1/servers/$id")

    suspend fun get(name: String) = list().singleOrNull { it.name == name }?.let {
        get(it.id)
    }

    suspend fun create(request: ServerCreateRequest) = api.post<ServerResponseWrapper>("v1/servers", request)
}
