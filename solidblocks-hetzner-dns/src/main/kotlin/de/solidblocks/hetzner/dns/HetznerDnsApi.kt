package de.solidblocks.hetzner.dns

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.solidblocks.hetzner.dns.model.ListZonesResponse
import de.solidblocks.hetzner.dns.model.RecordRequest
import de.solidblocks.hetzner.dns.model.RecordResponseWrapper
import de.solidblocks.hetzner.dns.model.RecordsResponseWrapper
import de.solidblocks.hetzner.dns.model.ZoneRequest
import de.solidblocks.hetzner.dns.model.ZoneResponseWrapper
import java.io.IOException
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

public class HetznerDnsApi(
    val apiKey: String,
    val baseApiUrl: String = "https://dns.hetzner.com/api/v1",
) {

  private val client = OkHttpClient.Builder().build()

  private val objectMapper =
      jacksonObjectMapper()
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .registerKotlinModule()
          .registerModules(
              JavaTimeModule(),
          )

  private inline fun <reified T> Call.executeAndParse(): Result<T> {
    try {
      this.execute().use { response ->
        if (!response.isSuccessful && response.code != 404) {
          return Result.failure(
              RuntimeException(
                  "http call failed with '${response.code}' for '${response.request.url}'"))
        }

        if (response.body == null) {
          return Result.failure(
              RuntimeException("response body was empty for '${response.request.url}'"))
        }

        val body = response.body!!.bytes()
        return Result.success(objectMapper.readValue(body))
      }
    } catch (e: IOException) {
      return Result.failure(e)
    }
  }

  private inline fun <reified T> get(path: String): Result<T> {
    val request: Request =
        Request.Builder().url("$baseApiUrl/$path").header("Auth-API-Token", apiKey).build()

    return client.newCall(request).executeAndParse()
  }

  private inline fun <reified T> post(path: String, body: Any): Result<T> {
    val request: Request =
        Request.Builder()
            .url("$baseApiUrl/$path")
            .header("Auth-API-Token", apiKey)
            .post(
                objectMapper
                    .writeValueAsString(body)
                    .toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

    return client.newCall(request).executeAndParse()
  }

  private inline fun <reified T> put(path: String, body: Any): Result<T> {
    val request: Request =
        Request.Builder()
            .url("$baseApiUrl/$path")
            .header("Auth-API-Token", apiKey)
            .put(
                objectMapper
                    .writeValueAsString(body)
                    .toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

    return client.newCall(request).executeAndParse()
  }

  private fun delete(path: String): Boolean {
    val request: Request =
        Request.Builder().url("$baseApiUrl/$path").header("Auth-API-Token", apiKey).delete().build()
    return client.newCall(request).execute().isSuccessful
  }

  fun zoneById(zoneId: String): Result<ZoneResponseWrapper> = get("zones/$zoneId")

  fun createZone(request: ZoneRequest): Result<ZoneResponseWrapper> = post("zones", request)

  fun updateZone(zoneId: String, request: ZoneRequest): Result<ZoneResponseWrapper> =
      put("zones/$zoneId", request)

  fun zones(name: String? = null): Result<ListZonesResponse> =
      get(
          if (name != null) {
            "zones?name=$name"
          } else {
            "zones"
          },
      )

  fun deleteZone(zoneId: String) = delete("zones/$zoneId")

  fun records(zoneId: String): Result<RecordsResponseWrapper> = get("records?zone_id=$zoneId")

  fun createRecord(record: RecordRequest): Result<RecordResponseWrapper> = post("records", record)

  fun updateRecord(recordId: String, record: RecordRequest): Result<RecordResponseWrapper> =
      put("records/$recordId", record)

  /*
  fun createRecords(record: RecordRequest): Result<RecordResponseWrapper>  {
      var entity =
          new HttpEntity<>(
                  BulkRecordsRequest.builder().records(Arrays.asList(request)).build(), httpHeaders);
      return request("/records/bulk", HttpMethod.POST, entity, ListRecordsResponse.class)
          .map(ListRecordsResponse::getRecords)
          .orElse(Collections.emptyList());
  }
   */
}
