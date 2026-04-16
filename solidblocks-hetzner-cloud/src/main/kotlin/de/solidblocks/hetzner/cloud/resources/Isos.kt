package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.Architecture
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.Serializable

open class IsoFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class IsoNameFilter(name: String) : IsoFilter("name", name)

class IsoArchitectureFilter(architecture: Architecture) : IsoFilter("architecture", architecture.toString())

class IsoIncludeArchitectureWildcardFilter(include: Boolean) : IsoFilter("include_architecture_wildcard", include.toString())

enum class IsoType {
    public,
    private,
}

@Serializable
data class IsosListResponseWrapper(val isos: List<IsoResponse>, override val meta: MetaResponse) : ListResponse<IsoResponse> {
    override val list: List<IsoResponse>
        get() = isos
}

@Serializable
data class IsoResponseWrapper(val iso: IsoResponse)

@Serializable
data class IsoResponse(
    override val id: Long,
    override val name: String?,
    val description: String,
    val type: IsoType,
    val architecture: String? = null,
) : HetznerNamedResource<Long>

class HetznerIsosApi(private val api: HetznerApi) : HetznerBaseResourceApi<IsoResponse, IsoFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<IsoFilter>, labelSelectors: Map<String, LabelSelectorValue>): IsosListResponseWrapper =
        api.get("v1/isos?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list ISOs")

    suspend fun get(id: Long) = api.get<IsoResponseWrapper>("v1/isos/$id")?.iso

    suspend fun get(name: String) = list(listOf(IsoNameFilter(name))).singleOrNull()
}
