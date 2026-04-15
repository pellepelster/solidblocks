package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class ServerTypeFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class ServerTypeNameFilter(name: String) : ServerTypeFilter("name", name)

@Serializable
data class ServerTypeListResponseWrapper(@SerialName("server_types") val serverTypes: List<ServerTypeResponse>, override val meta: MetaResponse) : ListResponse<ServerTypeResponse> {
    override val list: List<ServerTypeResponse>
        get() = serverTypes
}

@Serializable
data class ServerTypeResponseWrapper(@SerialName("server_type") val serverType: ServerTypeResponse)

@Serializable
data class ServerTypeResponse(val id: Long, val name: String, val description: String, val cores: Int, val memory: Int, val disk: Int)

class HetznerServerTypesApi(private val api: HetznerApi) : HetznerBaseResourceApi<ServerTypeResponse, ServerTypeFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<ServerTypeFilter>, labelSelectors: Map<String, LabelSelectorValue>): ServerTypeListResponseWrapper =
        api.get("v1/server_types?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list server types")

    suspend fun get(id: Long) = api.get<ServerTypeResponseWrapper>("v1/server_types/$id")?.serverType

    suspend fun get(name: String) = list(listOf(ServerTypeNameFilter(name))).singleOrNull()
}
