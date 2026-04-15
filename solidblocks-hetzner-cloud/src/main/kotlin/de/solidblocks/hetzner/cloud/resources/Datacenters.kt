package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.Serializable

open class DatacenterFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class DatacenterNameFilter(name: String) : DatacenterFilter("name", name)

@Serializable
data class DatacentersListResponseWrapper(val datacenters: List<DatacenterResponse>, override val meta: MetaResponse) : ListResponse<DatacenterResponse> {
    override val list: List<DatacenterResponse>
        get() = datacenters
}

@Serializable
data class DatacenterResponseWrapper(val datacenter: DatacenterResponse)

@Serializable
data class DatacenterResponse(
    override val id: Long,
    override val name: String,
    val description: String,
    val location: LocationResponse,
) : HetznerNamedResource<Long>

class HetznerDatacentersApi(private val api: HetznerApi) : HetznerBaseResourceApi<DatacenterResponse, DatacenterFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<DatacenterFilter>, labelSelectors: Map<String, LabelSelectorValue>): DatacentersListResponseWrapper =
        api.get("v1/datacenters?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list datacenters")

    suspend fun get(id: Long) = api.get<DatacenterResponseWrapper>("v1/datacenters/$id")?.datacenter

    suspend fun get(name: String) = list(listOf(DatacenterNameFilter(name))).singleOrNull()
}
