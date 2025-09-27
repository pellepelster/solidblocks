package de.solidblocks.hetzner.dns

import de.solidblocks.hetzner.dns.model.*
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

public class HetznerDnsApi(
    val apiKey: String,
    val baseApiUrl: String = "https://dns.hetzner.com/api/v1/",
) {

    class SolidblocksHttpLogger() : Logger {
        override fun log(message: String) {
            println(message)
        }
    }

    internal val client =
        createHttpClient(baseApiUrl, apiKey, System.getenv("BLCKS_DEBUG") != null)

    @OptIn(ExperimentalSerializationApi::class)
    fun createHttpClient(baseUrl: String, apiToken: String, debug: Boolean) =
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
                url(baseUrl)
                headers.append("Auth-API-Token", apiToken)
            }
        }


    internal suspend inline fun <reified T> get(path: String): T? = client.get(path).handle<T>()

    internal suspend inline fun <reified T> post(path: String, data: Any? = null): T? {
        return client
            .post(path) {
                contentType(ContentType.Application.Json)
                data?.let { this.setBody(it) }
            }
            .handle<T>()
    }

    internal suspend inline fun <reified T> put(path: String, data: Any? = null): T? {
        return client
            .put(path) {
                contentType(ContentType.Application.Json)
                data?.let { this.setBody(it) }
            }
            .handle<T>()
    }

    internal suspend inline fun <reified T> HttpResponse.handle(): T? {
        if (this.status.isSuccess()) {
            return this.body()
        }

        if (this.status.isNotFound()) {
            return null
        }

        if (this.status.isBadRequest()) {
            throw HetznerApiException(this.body())
        }

        throw RuntimeException("unexpected response HTTP ${this.status} (${this.bodyAsText()})")
    }

    suspend fun zoneById(zoneId: String): ZoneResponseWrapper? = get("zones/$zoneId")

    suspend fun createZone(request: ZoneRequest): ZoneResponseWrapper? = post("zones", request)

    suspend fun updateZone(zoneId: String, request: ZoneRequest): ZoneResponseWrapper? =
        put("zones/$zoneId", request)

    suspend fun zones(name: String? = null): ListZonesResponse? =
        get(
            if (name != null) {
                "zones?name=$name"
            } else {
                "zones"
            },
        )

    suspend fun deleteZone(zoneId: String) = !client.delete("zones/$zoneId").status.isNotFound()

    suspend fun records(zoneId: String): RecordsResponseWrapper? = get("records?zone_id=$zoneId")

    suspend fun createRecord(record: RecordRequest): RecordResponseWrapper? = post("records", record)

    suspend fun updateRecord(recordId: String, record: RecordRequest): RecordResponseWrapper? =
        put("records/$recordId", record)

    /*
    fun createRecords(record: RecordRequest): RecordResponseWrapper  {
        var entity =
            new HttpEntity<>(
                    BulkRecordsRequest.builder().records(Arrays.asList(request)).build(), httpHeaders);
        return request("/records/bulk", HttpMethod.POST, entity, ListRecordsResponse.class)
            .map(ListRecordsResponse::getRecords)
            .orElse(Collections.emptyList());
    }
     */
}

public fun HttpStatusCode.isNotFound(): Boolean = value == 404

public fun HttpStatusCode.isBadRequest(): Boolean = value in (400 until 500)
