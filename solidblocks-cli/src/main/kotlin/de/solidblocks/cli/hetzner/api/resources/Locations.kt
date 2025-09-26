package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerBaseResourceApi
import de.solidblocks.cli.hetzner.api.listQuery
import de.solidblocks.cli.hetzner.api.model.*
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

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): LocationsListResponseWrapper =
        api.get("v1/locations?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list locations")

    suspend fun get(id: Long) = api.get<LocationResponseWrapper>("v1/locations/$id")?.location

    suspend fun get(name: String) =
        list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

}
