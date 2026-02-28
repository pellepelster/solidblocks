package de.solidblocks.garagefs

import kotlinx.serialization.Serializable

@Serializable
data class CreateKeyRequest(
    val allow: KeyPerm? = null,
    val deny: KeyPerm? = null,
    val expiration: String? = null,
    val name: String? = null,
    val neverExpires: Boolean = true
)

@Serializable
data class KeyInfoBucketResponse(
    val globalAliases: List<String>,
    val id: String,
    val localAliases: List<String>,
    val permissions: BucketKeyPermRequest
)


@Serializable
data class KeyPerm(
    val createBucket: Boolean? = null
)

@Serializable
data class ListKeysResponse(
    val expired: Boolean,
    val id: String,
    val name: String,
)

@Serializable
data class KeyInfoResponse(
    val accessKeyId: String,
    val buckets: List<KeyInfoBucketResponse>,
    val expired: Boolean,
    val name: String,
    val permissions: KeyPerm,
    val secretAccessKey: String? = null
)


class AccessKeyApi(val api: GarageFsApi) {
    suspend fun listKeys() = api.get<List<ListKeysResponse>>("v2/ListKeys")
    suspend fun getKeyInfo(id: String, showSecretKey: Boolean = false) = api.get<KeyInfoResponse>("v2/GetKeyInfo?id=${id}&showSecretKey=${showSecretKey}")
    suspend fun createKey(request: CreateKeyRequest) = api.post<KeyInfoResponse>("v2/CreateKey", request)
}