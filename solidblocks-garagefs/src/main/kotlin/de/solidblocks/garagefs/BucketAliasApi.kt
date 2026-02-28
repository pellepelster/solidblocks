package de.solidblocks.garagefs

import kotlinx.serialization.Serializable

@Serializable
data class BucketAliasRequest(
    val bucketId: String,
    val globalAlias: String,
    val accessKeyId: String? = null,
    val localAlias: String? = null
)

class BucketAliasApi(val api: GarageFsApi) {
    suspend fun removeBucketAlias(request: BucketAliasRequest) = api.post<BucketInfoResponse>("v2/RemoveBucketAlias", request)
    suspend fun addBucketAlias(request: BucketAliasRequest) = api.post<BucketInfoResponse>("/v2/AddBucketAlias", request)
}