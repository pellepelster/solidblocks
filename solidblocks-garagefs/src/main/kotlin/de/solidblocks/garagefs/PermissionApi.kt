package de.solidblocks.garagefs

import kotlinx.serialization.Serializable

@Serializable
data class BucketKeyPermChangeRequest(
    val accessKeyId: String,
    val bucketId: String,
    val permissions: BucketKeyPermRequest
)

class PermissionApi(val api: GarageFsApi) {
    suspend fun allowBucketKey(request: BucketKeyPermChangeRequest) = api.post<BucketInfoResponse>("v2/AllowBucketKey", request)
    suspend fun denyBucketKey(request: BucketKeyPermChangeRequest) = api.post<BucketInfoResponse>("v2/DenyBucketKey", request)
}