package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
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
    HetznerDeleteResourceApi<VolumeResponse>, HetznerProtectedResourceApi<VolumeResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): VolumesListWrapper =
        api.get("v1/volumes?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list volumes")

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
data class VolumesListWrapper(val volumes: List<VolumeResponse>, override val meta: MetaResponse) :
    ListResponse<VolumeResponse> {
    override val list: List<VolumeResponse>
        get() = volumes
}
