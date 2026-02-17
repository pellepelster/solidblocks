package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.*
import de.solidblocks.hetzner.cloud.model.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val location: String,
    @SerialName("format") val format: VolumeFormat,
    val automount: Boolean = false,
    val labels: Map<String, String>? = null,
)

@Serializable data class VolumeResponseWrapper(@SerialName("volume") val volume: VolumeResponse)

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

class HetznerVolumesApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, VolumeResponse>,
    HetznerProtectedResourceApi<Long, VolumeResponse> {

  override suspend fun listPaged(
      page: Int,
      perPage: Int,
      filter: Map<String, FilterValue>,
      labelSelectors: Map<String, LabelSelectorValue>,
  ): VolumesListWrapper =
      api.get("v1/volumes?${listQuery(page, perPage, filter, labelSelectors)}")
          ?: throw RuntimeException("failed to list volumes")

  override suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper =
      api.post("v1/volumes/$id/actions/change_protection", ChangeVolumeProtectionRequest(delete))
          ?: throw RuntimeException("failed to change protection")

  suspend fun detach(id: Long): ActionResponseWrapper =
      api.post("v1/volumes/$id/actions/detach") ?: throw RuntimeException("failed to detach volume")

  override suspend fun action(id: Long): ActionResponseWrapper =
      api.get("v1/volumes/actions/$id") ?: throw RuntimeException("failed to get volume action")

  override suspend fun delete(id: Long) = api.simpleDelete("v1/volumes/$id")

  suspend fun get(id: Long) = api.get<VolumeResponseWrapper>("v1/volumes/$id")?.volume

  suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

  suspend fun create(request: VolumeCreateRequest) =
      api.post<VolumeResponseWrapper>("v1/volumes", request)

  suspend fun update(id: Long, request: VolumeUpdateRequest) =
      api.put<VolumeResponseWrapper>("v1/volumes/$id", request)

  suspend fun waitForAction(
      action: ActionResponseWrapper,
      logCallback: ((String) -> Unit)? = null,
  ) = waitForAction(action.action, logCallback)

  suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) =
      waitForAction(action.id, logCallback)

  suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) =
      api.waitForAction(id, logCallback, { api.volumes.action(it) })
}

@Serializable
data class VolumesListWrapper(val volumes: List<VolumeResponse>, override val meta: MetaResponse) :
    ListResponse<VolumeResponse> {
  override val list: List<VolumeResponse>
    get() = volumes
}
