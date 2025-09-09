package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerNamedResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ImageType {
    SYSTEM,
    SNAPSHOT,
}

@Serializable
data class ImagesListWrapper(
    @SerialName("images") val images: List<ImageResponse>,
    override val meta: Meta,
) : ListResponse<ImageResponse> {

    override val list: List<ImageResponse>
        get() = images
}

@Serializable
data class ImageResponse(override val id: Long, override val name: String?, val type: ImageType) :
    HetznerNamedResource

class HetznerImagesApi(private val api: HetznerApi) : HetznerDeleteResourceApi<ImageResponse> {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): ImagesListWrapper =
        api.get("v1/images?page=$page&per_page=$perPage&type=${ImageType.SNAPSHOT.name.lowercase()}")
            ?: throw RuntimeException("failed to list images")

    override suspend fun list() =
        api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/images/$id")
}
