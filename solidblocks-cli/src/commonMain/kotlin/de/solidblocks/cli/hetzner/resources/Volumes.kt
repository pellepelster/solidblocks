package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.HetznerApi
import de.solidblocks.cli.hetzner.HetznerProtectedResource
import de.solidblocks.cli.hetzner.HetznerProtectedResourceApi
import de.solidblocks.cli.hetzner.HetznerProtectionResponse
import de.solidblocks.cli.hetzner.HetznerSimpleResourceApi
import kotlinx.serialization.Serializable

@Serializable
data class VolumeResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    val server: Int?
) :
    HetznerProtectedResource

@Serializable
data class ChangeVolumeProtectionRequest(val delete: Boolean, val rebuild: Boolean? = null)


class HetznerVolumesApi(private val api: HetznerApi) : HetznerSimpleResourceApi<VolumeResponse>,
    HetznerProtectedResourceApi {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): VolumesListWrapper =
        api.get("v1/volumes?page=${page}&per_page=${perPage}")

    override suspend fun list() = api.handlePaginatedList { page, perPage ->
        listPaged(page, perPage)
    }

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post("v1/volumes/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete))

    suspend fun detach(id: Long): ActionResponseWrapper =
        api.post("v1/volumes/$id/actions/detach")

    override suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/volumes/actions/${id}")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/volumes/${id}")

}

@Serializable
data class VolumesListWrapper(val volumes: List<VolumeResponse>, override val meta: Meta) :
    ListResponse<VolumeResponse> {
    override val list: List<VolumeResponse>
        get() = volumes
}
