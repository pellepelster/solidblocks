package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.HetznerApi
import de.solidblocks.cli.hetzner.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.HetznerNamedResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ImageType {
  system,
  snapshot
}

@Serializable
data class ImagesListWrapper(
    @SerialName("images") val images: List<ImageResponse>,
    override val meta: Meta
) : ListResponse<ImageResponse> {

  override val list: List<ImageResponse>
    get() = images
}

@Serializable
data class ImageResponse(override val id: Long, override val name: String?, val type: ImageType) :
    HetznerNamedResource

class HetznerImagesApi(private val api: HetznerApi) : HetznerDeleteResourceApi<ImageResponse> {

  suspend fun listPaged(page: Int = 0, perPage: Int = 25): ImagesListWrapper =
      api.get("v1/images?page=${page}&per_page=${perPage}&type=${ImageType.snapshot}")

  override suspend fun list() =
      api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

  override suspend fun delete(id: Long) = api.simpleDelete("v1/images/${id}")
}
