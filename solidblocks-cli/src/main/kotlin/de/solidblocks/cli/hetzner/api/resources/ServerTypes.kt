package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
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

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): ServerTypeListResponseWrapper =
        api.get("v1/server_types?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list server types")

    override suspend fun list(filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>) =
        api.handlePaginatedList(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors
            )
        }

    suspend fun get(id: Long) = api.get<ServerTypeResponseWrapper>("v1/server_types/$id")

    suspend fun get(name: String) =
        list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()?.let {
            get(it.id)
        }

}
