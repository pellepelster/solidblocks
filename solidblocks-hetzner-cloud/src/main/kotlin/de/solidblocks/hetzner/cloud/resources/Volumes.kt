package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.HetznerProtectedResourceApi
import de.solidblocks.hetzner.cloud.InstantSerializer
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectedResource
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectionResponse
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

open class VolumeFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class VolumeNameFilter(name: String) : VolumeFilter("name", name)

class VolumeStatusFilter(status: VolumeStatus) : VolumeFilter("status", status.toString())

enum class VolumeFormat {
    ext4,
    xfs,
}

@Serializable
data class VolumeUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

@Serializable
data class VolumeCreateRequest(
    val name: String,
    val size: Int,
    val location: HetznerLocation,
    @SerialName("format") val format: VolumeFormat,
    val automount: Boolean,
    val labels: Map<String, String>? = null,
)

@Serializable
data class VolumeResponseWrapper(@SerialName("volume") val volume: VolumeResponse)

enum class VolumeStatus {
    available,
    creating,
}

@Serializable
data class VolumeResponse
@OptIn(ExperimentalTime::class)
constructor(
    override val id: Long,
    override val name: String,
    @SerialName("format") val format: VolumeFormat?,
    @SerialName("linux_device") val linuxDevice: String,
    val size: Int,
    override val protection: HetznerDeleteProtectionResponse,
    val server: Long?,
    val status: VolumeStatus,
    @Serializable(with = InstantSerializer::class) val created: Instant,
    val labels: Map<String, String>,
) : HetznerDeleteProtectedResource<Long>

@Serializable
data class ChangeVolumeProtectionRequest(val delete: Boolean, val rebuild: Boolean? = null)

@Serializable
data class VolumeAttachRequest(val server: Long, val automount: Boolean)

@Serializable
data class VolumeResizeRequest(val size: Int)

class HetznerVolumesApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, VolumeResponse, VolumeFilter>,
    HetznerProtectedResourceApi<Long, VolumeResponse, VolumeFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<VolumeFilter>, labelSelectors: Map<String, LabelSelectorValue>): VolumesListWrapper =
        api.get("v1/volumes?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list volumes")

    override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper = api.post("v1/volumes/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete))
        ?: throw RuntimeException("failed to change protection")

    suspend fun detach(id: Long): ActionResponseWrapper = api.post("v1/volumes/$id/actions/detach") ?: throw RuntimeException("failed to detach volume")

    suspend fun attach(id: Long, serverId: Long, automount: Boolean = false): ActionResponseWrapper = api.post(
        "v1/volumes/$id/actions/attach",
        VolumeAttachRequest(serverId, automount),
    ) ?: throw RuntimeException("failed to attach volume")

    suspend fun resize(id: Long, size: Int): ActionResponseWrapper = api.post(
        "v1/volumes/$id/actions/resize",
        VolumeResizeRequest(size),
    ) ?: throw RuntimeException("failed to resize volume")

    override suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/volumes/actions/$id") ?: throw RuntimeException("failed to get volume action")

    override suspend fun delete(id: Long) = api.delete("v1/volumes/$id")

    suspend fun get(id: Long) = api.get<VolumeResponseWrapper>("v1/volumes/$id")?.volume

    suspend fun get(name: String) = list(listOf(VolumeNameFilter(name))).singleOrNull()

    suspend fun create(request: VolumeCreateRequest): VolumeResponseWrapper = api.post<VolumeResponseWrapper>("v1/volumes", request)

    suspend fun update(id: Long, request: VolumeUpdateRequest) = api.put<VolumeResponseWrapper>("v1/volumes/$id", request)

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { api.volumes.action(it) })
}

@Serializable
data class VolumesListWrapper(val volumes: List<VolumeResponse>, override val meta: MetaResponse) : ListResponse<VolumeResponse> {
    override val list: List<VolumeResponse>
        get() = volumes
}
