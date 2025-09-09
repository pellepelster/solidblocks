package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import kotlinx.serialization.Serializable

@Serializable
data class VolumeResponse(
    override val id: Long,
    override val name: String,
    override val protection: HetznerProtectionResponse,
    val server: Int?,
) : HetznerProtectedResource

@Serializable
data class ChangeVolumeProtectionRequest(val delete: Boolean, val rebuild: Boolean? = null)

class HetznerVolumesApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<VolumeResponse>, HetznerProtectedResourceApi {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): VolumesListWrapper =
        api.get("v1/volumes?page=$page&per_page=$perPage") ?: throw RuntimeException("failed to list volumes")

    override suspend fun list() =
        api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

    override suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper =
        api.post("v1/volumes/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete))
            ?: throw RuntimeException("failed to change protection")

    suspend fun detach(id: Long): ActionResponseWrapper =
        api.post("v1/volumes/$id/actions/detach") ?: throw RuntimeException("failed to detach volume")

    override suspend fun action(id: Long): ActionResponseWrapper =
        api.get("v1/volumes/actions/$id") ?: throw RuntimeException("failed to get volume action")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/volumes/$id")
}

@Serializable
data class VolumesListWrapper(val volumes: List<VolumeResponse>, override val meta: Meta) :
    ListResponse<VolumeResponse> {
    override val list: List<VolumeResponse>
        get() = volumes
}
