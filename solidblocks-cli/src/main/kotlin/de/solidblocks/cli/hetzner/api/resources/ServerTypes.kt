package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import de.solidblocks.cli.hetzner.api.model.FilterValue
import de.solidblocks.cli.hetzner.api.model.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.model.ListResponse
import de.solidblocks.cli.hetzner.api.model.Meta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerTypeListResponseWrapper(
    @SerialName("server_types")
    val serverTypes: List<ServerTypeResponse>,
    override val meta: Meta,
) : ListResponse<ServerTypeResponse> {
    override val list: List<ServerTypeResponse>
        get() = serverTypes
}

@Serializable
data class ServerTypeResponseWrapper(
    @SerialName("server_type")
    val serverType: ServerTypeResponse,
)

@Serializable
data class ServerTypeResponse(val id: Long, val name: String)

class HetznerServerTypesApi(private val api: HetznerApi) :
    HetznerBaseResourceApi<ServerTypeResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): ServerTypeListResponseWrapper =
        api.get("v1/server_types?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list server types")

    suspend fun get(id: Long) = api.get<ServerTypeResponseWrapper>("v1/server_types/$id")?.serverType

    suspend fun get(name: String) =
        list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

}
