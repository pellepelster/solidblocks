package de.solidblocks.cloud.github

import de.solidblocks.cloud.github.resources.GitHubRunnersApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubApiError(val message: String, @SerialName("documentation_url") val documentationUrl: String? = null)

class GitHubApiException(val error: GitHubApiError) : RuntimeException(error.message)

class GitHubApi(token: String) {
    val runners = GitHubRunnersApi(this)

    internal val client = createHttpClient(token)

    @OptIn(ExperimentalSerializationApi::class)
    private fun createHttpClient(token: String) = HttpClient(Java) {
        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
        defaultRequest {
            url("https://api.github.com/")
            headers.append("Authorization", "Bearer $token")
            headers.append("Accept", "application/vnd.github+json")
            headers.append("X-GitHub-Api-Version", "2022-11-28")
        }
    }

    internal suspend inline fun <reified T> post(path: String, data: Any? = null): T = client
        .post(path) {
            contentType(ContentType.Application.Json)
            data?.let { setBody(it) }
        }.handle()

    internal suspend inline fun <reified T> get(path: String): T = client.get(path).handle()

    internal suspend fun delete(path: String) {
        val response = client.delete(path)
        if (!response.status.isSuccess()) {
            if (response.status == HttpStatusCode.UnprocessableEntity || response.status.value in (400 until 500)) {
                val error: GitHubApiError = response.body()
                throw GitHubApiException(error)
            }
            throw RuntimeException("unexpected response HTTP ${response.status} (${response.bodyAsText()})")
        }
    }

    internal suspend inline fun <reified T> HttpResponse.handle(): T {
        if (status.isSuccess()) {
            return body()
        }

        if (status == HttpStatusCode.UnprocessableEntity || status.value in (400 until 500)) {
            val error: GitHubApiError = body()
            throw GitHubApiException(error)
        }

        throw RuntimeException("unexpected response HTTP $status (${bodyAsText()})")
    }
}
