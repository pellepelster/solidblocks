package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.Constants.defaultPageSize
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerApiErrorType
import de.solidblocks.hetzner.cloud.model.HetznerApiErrorWrapper
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.resources.ActionResponseWrapper
import de.solidblocks.hetzner.cloud.resources.ActionStatus
import de.solidblocks.hetzner.cloud.resources.HetznerCertificatesApi
import de.solidblocks.hetzner.cloud.resources.HetznerDatacentersApi
import de.solidblocks.hetzner.cloud.resources.HetznerDnsRRSetsApi
import de.solidblocks.hetzner.cloud.resources.HetznerDnsZonesApi
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallsApi
import de.solidblocks.hetzner.cloud.resources.HetznerFloatingIpsApi
import de.solidblocks.hetzner.cloud.resources.HetznerImagesApi
import de.solidblocks.hetzner.cloud.resources.HetznerIsosApi
import de.solidblocks.hetzner.cloud.resources.HetznerLoadBalancersApi
import de.solidblocks.hetzner.cloud.resources.HetznerLocationsApi
import de.solidblocks.hetzner.cloud.resources.HetznerNetworksApi
import de.solidblocks.hetzner.cloud.resources.HetznerPlacementGroupsApi
import de.solidblocks.hetzner.cloud.resources.HetznerPricingApi
import de.solidblocks.hetzner.cloud.resources.HetznerPrimaryIpsApi
import de.solidblocks.hetzner.cloud.resources.HetznerSSHKeysApi
import de.solidblocks.hetzner.cloud.resources.HetznerServerTypesApi
import de.solidblocks.hetzner.cloud.resources.HetznerServersApi
import de.solidblocks.hetzner.cloud.resources.HetznerVolumesApi
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

suspend fun <T> retryUtil(
    block: suspend () -> T?,
    condition: (T) -> Boolean,
    times: Int = 40,
    initialDelay: Long = 1000,
    maxDelay: Long = 5000,
    factor: Double = 2.0,
): T? {
    var currentDelay = initialDelay

    repeat(times - 1) {
        try {
            val result = block()
            if (result == null) {
                return null
            }

            if (condition(result)) {
                return result
            }
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

fun listQuery(page: Int = 0, perPage: Int = 25, filter: List<BaseFilter>, labelSelector: Map<String, LabelSelectorValue>): String {
    val labelSelector =
        if (labelSelector.isNotEmpty()) {
            "label_selector=${
                labelSelector.entries.joinToString(",") {
                    it.value.query(it.key)
                }
            }"
        } else {
            null
        }

    val filter = filter.joinToString("&") { it.queryPart() }
    val page = "page=$page"
    val perPage = "per_page=$perPage"

    return listOfNotNull(page, perPage, filter, labelSelector).joinToString("&")
}

public class HetznerApi(hcloudToken: String) {
    val volumes = HetznerVolumesApi(this)
    val servers = HetznerServersApi(this)
    val serverTypes = HetznerServerTypesApi(this)
    val locations = HetznerLocationsApi(this)
    val certificates = HetznerCertificatesApi(this)
    val sshKeys = HetznerSSHKeysApi(this)
    val networks = HetznerNetworksApi(this)
    val dnsZones = HetznerDnsZonesApi(this)
    val firewalls = HetznerFirewallsApi(this)
    val loadBalancers = HetznerLoadBalancersApi(this)
    val floatingIps = HetznerFloatingIpsApi(this)
    val primaryIps = HetznerPrimaryIpsApi(this)
    val placementGroups = HetznerPlacementGroupsApi(this)
    val images = HetznerImagesApi(this)
    val datacenters = HetznerDatacentersApi(this)
    val isos = HetznerIsosApi(this)
    val pricing = HetznerPricingApi(this)

    fun dnsRrSets(dnsZoneReference: String) = HetznerDnsRRSetsApi(this, dnsZoneReference)

    internal val client = createHttpClient(
        "https://api.hetzner.cloud",
        hcloudToken,
        System.getenv("BLCKS_DEBUG") != null,
    )

    class SolidblocksHttpLogger : Logger {
        override fun log(message: String) {
            println(message)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun createHttpClient(url: String, apiToken: String, debug: Boolean) = HttpClient(Java) {
        if (debug) {
            install(Logging) {
                logger = SolidblocksHttpLogger()
                level = LogLevel.BODY
            }
        }
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

    internal suspend inline fun <reified T> post(path: String, data: Any? = null): T = client
        .post(path) {
            contentType(ContentType.Application.Json)
            data?.let {
                this.setBody(it)
            }
        }
        .handle<T>()

    internal suspend inline fun <reified T> put(path: String, data: Any? = null): T = client
        .put(path) {
            contentType(ContentType.Application.Json)
            data?.let { this.setBody(it) }
        }
        .handle<T>()

    internal suspend inline fun <reified T> get(path: String): T? = client.get(path).handleOptional<T>()

    internal suspend inline fun <reified T> deleteWithAction(path: String): T? = client.delete(path).handleOptional()

    internal suspend fun delete(path: String): Boolean = client.delete(path).handleDelete()

    private suspend fun HttpResponse.handleDelete(): Boolean {
        if (this.status.isSuccess()) {
            return true
        }

        if (this.status.isBadRequest()) {
            val error: HetznerApiErrorWrapper = this.body()
            logger.error { error }
            throw HetznerApiException(error.error, this.request.url)
        }

        throw RuntimeException("unexpected response HTTP ${this.status} (${this.bodyAsText()})")
    }

    private val logger = KotlinLogging.logger {}

    internal suspend inline fun <reified T> HttpResponse.handleOptional(): T? {
        if (this.status.isSuccess()) {
            return this.body()
        }

        if (this.status.isNotFound()) {
            return null
        }

        if (this.status.isBadRequest()) {
            val error: HetznerApiErrorWrapper = this.body()
            logger.error { error }
            throw HetznerApiException(error.error, this.request.url)
        }

        throw RuntimeException("unexpected response HTTP ${this.status} (${this.bodyAsText()})")
    }

    internal suspend inline fun <reified T> HttpResponse.handle(): T {
        if (this.status.isSuccess()) {
            return this.body()
        }

        val error: HetznerApiErrorWrapper = this.body()
        logger.error { error }
        throw HetznerApiException(error.error, this.request.url)
    }

    fun waitForAction(action: suspend () -> ActionResponseWrapper, getAction: suspend (Long) -> ActionResponseWrapper?) = runBlocking {
        val response = action.invoke()

        val result =
            retryUtil(
                block = { getAction.invoke(response.action.id) },
                condition = { it.action.status == ActionStatus.SUCCESS },
            ) ?: return@runBlocking true

        result.action.status == ActionStatus.SUCCESS
    }

    fun waitForAction(id: Long, logCallback: ((String) -> Unit)?, getAction: suspend (Long) -> ActionResponseWrapper?) = runBlocking {
        val result =
            retryUtil(
                block = {
                    getAction.invoke(id).also {
                        if (it != null) {
                            logCallback?.invoke(
                                "waiting for '${it.action.command}' to finish current status is '${it.action.status.name.lowercase()}'",
                            )
                        }
                    }
                },
                condition = { it.action.status == ActionStatus.SUCCESS },
            ) ?: return@runBlocking true

        result.action.status == ActionStatus.SUCCESS
    }
}

public fun HttpStatusCode.isNotFound(): Boolean = value == 404

public fun HttpStatusCode.isBadRequest(): Boolean = value in (400 until 500)

suspend fun <T, P : BaseFilter> handlePaginatedList(
    filter: List<P>,
    labelSelectors: Map<String, LabelSelectorValue>,
    block:
    suspend (
        page: Int,
        perPage: Int,
        list: List<P>,
        labelSelectors: Map<String, LabelSelectorValue>,
    ) -> ListResponse<T>,
): List<T> {
    var currentPage: Int? = 0
    val result = mutableListOf<T>()

    while (currentPage != null) {
        val response: ListResponse<T> = block(currentPage, defaultPageSize, filter, labelSelectors)
        result.addAll(response.list)
        currentPage = response.meta.pagination.next_page
    }

    return result.toList()
}
