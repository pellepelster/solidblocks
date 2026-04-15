package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.Architecture
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class ImageFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class ImageNameFilter(name: String) : ImageFilter("name", name)

class ImageTypeFilter(type: ImageType) : ImageFilter("type", type.toString().lowercase())

class ImageStatusFilter(status: ImageStatus) : ImageFilter("status", status.toString())

class ImageBoundToFilter(serverId: Long) : ImageFilter("bound_to", serverId.toString())

class ImageArchitectureFilter(architecture: Architecture) : ImageFilter("architecture", architecture.toString())

class ImageIncludeDeprecatedFilter : ImageFilter("include_deprecated", "true")

enum class ImageType {
    system,
    snapshot,
    app,
    backup,
}

enum class ImageStatus {
    available,
    creating,
    deleting,
    disabled,
}

@Serializable
data class ImagesListResponseWrapper(@SerialName("images") val images: List<ImageResponse>, override val meta: MetaResponse) : ListResponse<ImageResponse> {
    override val list: List<ImageResponse>
        get() = images
}

@Serializable
data class ImageResponseWrapper(val image: ImageResponse)

@Serializable
data class ImageResponse(
    override val id: Long,
    override val name: String?,
    val type: ImageType,
    val labels: Map<String, String> = emptyMap(),
    val description: String? = null,
) : HetznerNamedResource<Long>

@Serializable
data class ImageUpdateRequest(val description: String? = null, val type: ImageType? = null, val labels: Map<String, String>? = null)

@Serializable
data class ImageChangeProtectionRequest(val delete: Boolean)

class HetznerImagesApi(private val api: HetznerApi) : HetznerDeleteResourceApi<Long, ImageResponse, ImageFilter> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: List<ImageFilter>, labelSelectors: Map<String, LabelSelectorValue>): ImagesListResponseWrapper =
        api.get("v1/images?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list images")

    override suspend fun delete(id: Long) = api.delete("v1/images/$id")

    suspend fun get(id: Long) = api.get<ImageResponseWrapper>("v1/images/$id")?.image

    suspend fun get(name: String, filter: List<ImageFilter> = emptyList()) = list(listOf(ImageNameFilter(name)) + filter).singleOrNull()

    suspend fun update(id: Long, request: ImageUpdateRequest) = api.put<ImageResponseWrapper>("v1/images/$id", request)

    suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper = api.post(
        "v1/images/$id/actions/change_protection",
        ImageChangeProtectionRequest(delete),
    ) ?: throw RuntimeException("failed to change image protection")

    suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/images/actions/$id")
        ?: throw RuntimeException("failed to get image action")

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { api.images.action(it) })

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)
}
