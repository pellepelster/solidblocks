package de.solidblocks.cli.hetzner.api

import de.solidblocks.cli.hetzner.api.resources.*
import de.solidblocks.cli.utils.logInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

suspend fun <T> retryUtil(
    times: Int = 5,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T,
    condition: (T) -> Boolean,
): T {
    var currentDelay = initialDelay

    repeat(times - 1) {
        try {
            val result = block()
            return result
        } catch (exception: HetznerApiException) {
            if (exception.error.code != HetznerApiErrorType.RATE_LIMIT_EXCEEDED) {
                throw exception
            }
        }

        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }

    return block()
}

@OptIn(ExperimentalSerializationApi::class)
fun createHttpClient(url: String, apiToken: String) =
    HttpClient(Java) {
        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    decodeEnumsCaseInsensitive = true
                },
            )
        }

        defaultRequest {
            url(url)
            headers.append("Authorization", "Bearer $apiToken")
        }
    }

public class HetznerApi(hcloudToken: String, private val defaultPageSize: Int = 5) {

    val volumes = HetznerVolumesApi(this)
    val servers = HetznerServersApi(this)
    val certificates = HetznerCertificatesApi(this)
    val sshKeys = HetznerSSHKeysApi(this)
    val networks = HetznerNetworksApi(this)
    val firewalls = HetznerFirewallsApi(this)
    val loadBalancers = HetznerLoadBalancersApi(this)
    val floatingIps = HetznerFloatingIpsApi(this)
    val primaryIps = HetznerPrimaryIpsApi(this)
    val placementGroups = HetznerPlacementGroupsApi(this)
    val images = HetznerImagesApi(this)

    internal val client = createHttpClient("https://api.hetzner.cloud", hcloudToken)

    internal suspend inline fun <reified T> post(path: String, data: Any? = null): T? =
        client
            .post(path) {
                contentType(ContentType.Application.Json)
                data?.let { this.setBody(it) }
            }
            .handle<T>()

    internal suspend inline fun <reified T> get(path: String): T? = client.get(path).handle<T>()

    internal suspend fun simpleDelete(path: String): Boolean =
        client.delete(path).handleSimpleDelete()

    internal suspend inline fun <reified T> complexDelete(path: String): T? =
        client.delete(path).handle()

    private suspend fun HttpResponse.handleSimpleDelete(): Boolean {
        if (this.status.isSuccess()) {
            return true
        }

        if (this.status.isBadRequest()) {
            val error: HetznerApiErrorWrapper = this.body()
            throw HetznerApiException(error.error, this.request.url)
        }

        throw RuntimeException("unexpected response HTTP ${this.status} (${this.bodyAsText()})")
    }

    internal suspend inline fun <reified T> HttpResponse.handle(): T? {
        if (this.status.isSuccess()) {
            return this.body()
        }

        if (this.status.isNotFound()) {
            return null
        }

        if (this.status.isBadRequest()) {
            val error: HetznerApiErrorWrapper = this.body()
            throw HetznerApiException(error.error, this.request.url)
        }

        throw RuntimeException("unexpected response HTTP ${this.status} (${this.bodyAsText()})")
    }

    suspend fun <T> handlePaginatedList(
        block: suspend (page: Int, perPage: Int) -> ListResponse<T>,
    ): List<T> {
        var currentPage: Int? = 0
        val result = mutableListOf<T>()

        while (currentPage != null) {
            val response: ListResponse<T> = block(currentPage, defaultPageSize)
            result.addAll(response.list)
            currentPage = response.meta.pagination.next_page
        }

        return result.toList()
    }

    fun waitFor(
        action: suspend () -> ActionResponseWrapper,
        getAction: suspend (Long) -> ActionResponseWrapper,
    ) = runBlocking {
        val response = action.invoke()

        val result =
            retryUtil(
                block = {
                    val response = getAction.invoke(response.action.id)
                    logInfo(
                        "waiting for '${response.action.command}' to finish for resources ${
                            response.action.resources.joinToString(
                                ", ",
                            ) { "${it.type} ${it.id}" }
                        }",
                    )
                    response
                },
                condition = { it.action.status == ActionStatus.SUCCESS },
            )

        result.action.status == ActionStatus.SUCCESS
    }
}

public fun HttpStatusCode.isNotFound(): Boolean = value == 404

public fun HttpStatusCode.isBadRequest(): Boolean = value in (400 until 500)
