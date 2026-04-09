package de.solidblocks.garagefs

import kotlinx.serialization.Serializable

@Serializable
public data class UpdateBucketWebsiteAccess(val enabled: Boolean, val errorDocument: String? = null, val indexDocument: String? = null)

@Serializable
public data class UpdateBucketRequest(val quotas: ApiBucketQuotas? = null, val websiteAccess: UpdateBucketWebsiteAccess? = null)

@Serializable
public data class CreateBucketLocalAlias(val accessKeyId: String, val alias: String, val allow: BucketKeyPermRequest? = null)

@Serializable
public data class CreateBucketRequest(val globalAlias: String? = null, val localAlias: CreateBucketLocalAlias? = null)

@Serializable
public data class ListBucketsResponse(val globalAliases: List<String>, val id: String, val localAliases: List<BucketLocalAliasResponse>)

@Serializable public data class BucketLocalAliasResponse(val accessKeyId: String, val alias: String)

@Serializable
public data class BucketKeyPermRequest(val owner: Boolean? = null, val read: Boolean? = null, val write: Boolean? = null)

@Serializable
data class GetBucketInfoKey(val accessKeyId: String, val bucketLocalAliases: List<String>, val name: String, val permissions: BucketKeyPermRequest)

@Serializable data class ApiBucketQuotas(val maxObjects: Long? = null, val maxSize: Long? = null)

@Serializable
data class GetBucketInfoWebsiteResponse(val indexDocument: String, val errorDocument: String? = null)

@Serializable
data class BucketInfoResponse(
    val bytes: Long,
    val globalAliases: List<String>,
    val id: String,
    val propertyKeys: List<GetBucketInfoKey>? = null,
    val objects: Long,
    val quotas: ApiBucketQuotas? = null,
    val unfinishedMultipartUploadBytes: Long,
    val unfinishedMultipartUploadParts: Long,
    val unfinishedMultipartUploads: Long,
    val unfinishedUploads: Long,
    val websiteAccess: Boolean,
    val websiteConfig: GetBucketInfoWebsiteResponse? = null,
)

class BucketApi(val api: GarageFsApi) {
    suspend fun listBuckets() = api.get<List<ListBucketsResponse>>("v2/ListBuckets")

    suspend fun getBucketInfo(id: String) = api.get<BucketInfoResponse>("v2/GetBucketInfo?id=$id")

    suspend fun createBucket(request: CreateBucketRequest) = api.post<BucketInfoResponse>("v2/CreateBucket", request)

    suspend fun updateBucket(id: String, request: UpdateBucketRequest) = api.post<BucketInfoResponse>("v2/UpdateBucket?id=$id", request)
}
