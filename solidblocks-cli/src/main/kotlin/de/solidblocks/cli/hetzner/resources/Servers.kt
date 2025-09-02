package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.*
import kotlinx.serialization.Serializable

@Serializable
data class ServersListWrapper(val servers: List<ServerResponse>, override val meta: Meta) :
    ListResponse<ServerResponse> {
    override val list: List<ServerResponse>
        get() = servers
}

@Serializable
data class ServerResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
) : HetznerProtectedResource


class HetznerServersApi(private val api: HetznerApi) : HetznerDeleteWithActionResourceApi<ServerResponse>, HetznerProtectedResourceApi {
    suspend fun create(): ServersListWrapper = api.post("v1/servers", "")

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): ServersListWrapper =
        api.get("v1/servers?page=${page}&per_page=${perPage}")

    override suspend fun delete(id: Long): ActionResponseWrapper = api.complexDelete("v1/servers/${id}")

    override suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/servers/actions/${id}")

    override suspend fun list() = api.handlePaginatedList { page, perPage ->
        listPaged(page, perPage)
    }

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post("v1/servers/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete, delete))

}


