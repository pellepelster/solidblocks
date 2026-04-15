package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class LocationFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class LocationNameFilter(name: String) : LocationFilter("name", name)

@Serializable
data class LocationsListResponseWrapper(val locations: List<LocationResponse>, override val meta: MetaResponse) : ListResponse<LocationResponse> {
    override val list: List<LocationResponse>
        get() = locations
}

@Serializable
data class LocationResponseWrapper(val location: LocationResponse)

@Serializable
data class LocationResponse(override val id: Long, override val name: String, val description: String, val country: String, val city: String, @SerialName("network_zone") val networkZone: String) :
    HetznerNamedResource<Long>

class HetznerLocationsApi(private val api: HetznerApi) : HetznerBaseResourceApi<LocationResponse, LocationFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<LocationFilter>, labelSelectors: Map<String, LabelSelectorValue>): LocationsListResponseWrapper =
        api.get("v1/locations?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list locations")

    suspend fun get(id: Long) = api.get<LocationResponseWrapper>("v1/locations/$id")?.location

    suspend fun get(name: String) = list(listOf(LocationNameFilter(name))).singleOrNull()
}
