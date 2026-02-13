package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.Constants.defaultPageSize
import de.solidblocks.hetzner.cloud.model.*
import de.solidblocks.hetzner.cloud.resources.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

suspend fun <T> retryUtil(
    block: suspend () -> T,
    condition: (T) -> Boolean,
    times: Int = 20,
    initialDelay: Long = 1000,
    maxDelay: Long = 5000,
    factor: Double = 2.0,
): T {
  var currentDelay = initialDelay

  repeat(times - 1) {
    try {
      val result = block()

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

fun listQuery(
    page: Int = 0,
    perPage: Int = 25,
    filter: Map<String, FilterValue>,
    labelSelector: Map<String, LabelSelectorValue>,
): String {
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

  val filter = filter.entries.joinToString("&") { "${it.key}=${it.value.query}" }
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

  fun dnsRrSets(dnsZoneReference: String) = HetznerDnsRRSetsApi(this, dnsZoneReference)

  internal val client =
      createHttpClient(
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
  fun createHttpClient(url: String, apiToken: String, debug: Boolean) =
      HttpClient(Java) {
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

  internal suspend inline fun <reified T> post(path: String, data: Any? = null): T? =
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
      logger.error { error }
      throw HetznerApiException(error.error, this.request.url)
    }

    throw RuntimeException("unexpected response HTTP ${this.status} (${this.bodyAsText()})")
  }

  private val logger = KotlinLogging.logger {}

  internal suspend inline fun <reified T> HttpResponse.handle(): T? {
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

  fun waitForAction(
      action: suspend () -> ActionResponseWrapper,
      getAction: suspend (Long) -> ActionResponseWrapper,
  ) = runBlocking {
    val response = action.invoke()

    val result =
        retryUtil(
            block = { getAction.invoke(response.action.id) },
            condition = { it.action.status == ActionStatus.SUCCESS },
        )

    result.action.status == ActionStatus.SUCCESS
  }

  fun waitForAction(
      id: Long,
      logCallback: ((String) -> Unit)?,
      getAction: suspend (Long) -> ActionResponseWrapper,
  ) = runBlocking {
    val result =
        retryUtil(
            block = {
              getAction.invoke(id).also {
                logCallback?.invoke(
                    "waiting for '${it.action.command}' to finish current status is '${it.action.status.name.lowercase()}'",
                )
              }
            },
            condition = { it.action.status == ActionStatus.SUCCESS },
        )

    result.action.status == ActionStatus.SUCCESS
  }
}

public fun HttpStatusCode.isNotFound(): Boolean = value == 404

public fun HttpStatusCode.isBadRequest(): Boolean = value in (400 until 500)

suspend fun <T> handlePaginatedList(
    filter: Map<String, FilterValue>,
    labelSelectors: Map<String, LabelSelectorValue>,
    block:
        suspend (
            page: Int,
            perPage: Int,
            filter: Map<String, FilterValue>,
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
