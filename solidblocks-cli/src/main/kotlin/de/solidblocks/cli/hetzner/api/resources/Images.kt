package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import de.solidblocks.cli.hetzner.api.model.FilterValue
import de.solidblocks.cli.hetzner.api.model.HetznerNamedResource
import de.solidblocks.cli.hetzner.api.model.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.model.ListResponse
import de.solidblocks.cli.hetzner.api.model.Meta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ImageType {
    SYSTEM,
    SNAPSHOT,
    APP,
}

@Serializable
data class ImagesListResponseWrapper(
    @SerialName("images") val images: List<ImageResponse>,
    override val meta: Meta,
) : ListResponse<ImageResponse> {

    override val list: List<ImageResponse>
        get() = images
}

@Serializable
data class ImageResponseWrapper(
    val image: ImageResponse,
)

@Serializable
data class ImageResponse(override val id: Long, override val name: String?, val type: ImageType) :
    HetznerNamedResource

class HetznerImagesApi(private val api: HetznerApi) : HetznerDeleteResourceApi<ImageResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): ImagesListResponseWrapper =
        api.get("v1/images?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list images")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/images/$id")

    suspend fun get(id: Long) = api.get<ImageResponseWrapper>("v1/images/$id")?.image

    suspend fun get(name: String, filter: Map<String, FilterValue> = emptyMap()) =
        list(mapOf("name" to FilterValue.Equals(name)) + filter).singleOrNull()

}
