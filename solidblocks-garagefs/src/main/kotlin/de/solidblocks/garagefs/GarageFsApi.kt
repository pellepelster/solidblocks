package de.solidblocks.garagefs

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ClusterStatusNodeResponse(val id: String)

@Serializable
data class ClusterLayoutNodeResponse(
    val capacity: Long,
    val id: String,
    val zone: String,
    val tags: List<String>,
)

@Serializable
data class ClusterLayoutNodeRequest(
    val id: String,
    val capacity: Long,
    val tags: List<String> = emptyList(),
    val zone: String,
)

@Serializable
data class ClusterStatusResponse(val nodes: List<ClusterStatusNodeResponse>)


class GarageFsApi(val apiToken: String, val baseAddress: String = "http://localhost:3903") {

    val layoutApi = ClusterLayoutApi(this)
    val bucketApi = BucketApi(this)
    val bucketAliasApi = BucketAliasApi(this)
    val accessKeyApi = AccessKeyApi(this)
    val permissionApi = PermissionApi(this)

    internal val client =
        createHttpClient(
            baseAddress,
            apiToken,
        )

    public fun HttpStatusCode.isNotFound(): Boolean = value == 404

    public fun HttpStatusCode.isBadRequest(): Boolean = value in (400 until 500)


    @OptIn(ExperimentalSerializationApi::class)
    fun createHttpClient(url: String, apiToken: String) =
        HttpClient(Java) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        decodeEnumsCaseInsensitive = true
                        encodeDefaults = true
                    },
                )
            }

            defaultRequest {
                url(url)
                headers.append("Authorization", "Bearer $apiToken")
            }
        }

    internal suspend inline fun <reified T> get(path: String): T = client.get(path).handle<T>()

    internal suspend inline fun <reified T> HttpResponse.handle(): T {
        if (this.status.isSuccess()) {
            return this.body()
        }

        if (this.status.isNotFound()) {
            throw RuntimeException("'${this.request.url}' not found")
        }

        if (this.status.isBadRequest()) {
            val error: String = this.body()
            throw RuntimeException(error)
        }

        throw RuntimeException("unexpected response HTTP ${this.status} (${this.bodyAsText()})")
    }

    internal suspend inline fun <reified T> post(path: String, data: Any? = null): T =
        client
            .post(path) {
                contentType(ContentType.Application.Json)
                data?.let { this.setBody(it) }
            }
            .handle<T>()

    internal suspend inline fun <reified T> put(path: String, data: Any? = null): T? =
        client
            .put(path) {
                contentType(ContentType.Application.Json)
                data?.let { this.setBody(it) }
            }
            .handle<T>()

    suspend fun getClusterStatus() = get<ClusterStatusResponse>("v2/GetClusterStatus")

}