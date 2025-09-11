package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import kotlinx.serialization.Serializable

@Serializable
data class LocationsListResponseWrapper(
    val locations: List<LocationResponse>,
    override val meta: Meta,
) : ListResponse<LocationResponse> {
    override val list: List<LocationResponse>
        get() = locations
}

@Serializable
data class LocationResponseWrapper(
    val location: LocationResponse,
)

@Serializable
data class LocationResponse(override val id: Long, override val name: String) :
    HetznerNamedResource

class HetznerLocationsApi(private val api: HetznerApi) :
    HetznerBaseResourceApi<LocationResponse> {

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): LocationsListResponseWrapper =
        api.get("v1/locations?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list locations")

    override suspend fun list(filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>) =
        api.handlePaginatedList(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors
            )
        }

    suspend fun get(id: Long) = api.get<LocationResponseWrapper>("v1/locations/$id")

    suspend fun get(name: String) =
        list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()?.let {
            get(it.id)
        }

}
