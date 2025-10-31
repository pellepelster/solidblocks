package de.solidblocks.hetzner.dns

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import de.solidblocks.hetzner.dns.model.ZoneRequest
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import java.net.ConnectException
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import org.junit.jupiter.api.Test

@WireMockTest
@OptIn(ExperimentalTime::class)
class HetznerDnsAPITest {

  @Test
  fun testConnectFailure() {
    val dnsApi = HetznerDnsApi("api-token", "http://localhost:1234/api/v1")

    runBlocking { shouldThrow<ConnectException> { dnsApi.zoneById("1234") } }
  }

  @Test
  fun testApiTokenIsProvided(wiremock: WireMockRuntimeInfo) {
    val dnsApi = HetznerDnsApi("api-token", "http://localhost:${wiremock.httpPort}/api/v1/")

    stubFor(
        get(urlEqualTo("/api/v1/zones/1234"))
            .willReturn(
                okJson(
                    """{
                          "zone": {
                            "id": "1234",
                            "name": "zone1",
                            "owner": "owner1",
                            "project": "project1",
                            "status": "status1",
                            "created": "2022-09-03 18:38:52.597 +0000 UTC"
                          }
                        }
                        """
                        .trimIndent(),
                ),
            ),
    )

    runBlocking {
      assertSoftly(dnsApi.zoneById("1234")!!) {
        it.zone.id shouldBe "1234"
        it.zone.created shouldBe Instant.parse("2022-09-03T18:38:52.597Z").toKotlinInstant()
      }
    }

    verify(
        getRequestedFor(urlMatching("/api/v1/zones/1234"))
            .withHeader("Auth-API-Token", matching("api-token")),
    )
  }

  @Test
  fun testUnknownFieldsAreIgnored(wiremock: WireMockRuntimeInfo) {
    val dnsApi = HetznerDnsApi("api-token", "http://localhost:${wiremock.httpPort}/api/v1/")

    stubFor(
        get(urlEqualTo("/api/v1/zones/1234"))
            .willReturn(
                okJson(
                    """{
                          "zone": {
                            "id": "1234",
                            "name": "zone1",
                            "owner": "owner1",
                            "project": "project1",
                            "status": "status1",
                            "created": "2022-09-03 18:38:52.597 +0000 UTC",
                            "unknown_field": "foo-bar"
                          }
                        }
                        """
                        .trimIndent(),
                ),
            ),
    )

    runBlocking {
      assertSoftly(dnsApi.zoneById("1234")!!) {
        it.zone.id shouldBe "1234"
        it.zone.created shouldBe Instant.parse("2022-09-03T18:38:52.597Z").toKotlinInstant()
      }
    }

    verify(
        getRequestedFor(urlMatching("/api/v1/zones/1234"))
            .withHeader("Auth-API-Token", matching("api-token")),
    )
  }

  @Test
  fun testNullFieldsAreNotSent(wiremock: WireMockRuntimeInfo) {
    val dnsApi = HetznerDnsApi("api-token", "http://localhost:${wiremock.httpPort}/api/v1/")

    stubFor(
        post(urlEqualTo("/api/v1/zones/1234"))
            .willReturn(
                okJson(
                    """{
                          "zone": {
                            "id": "1234",
                            "name": "zone1",
                            "created": "2022-09-03 18:38:52.597 +0000 UTC"
                          }
                        }
                        """
                        .trimIndent(),
                ),
            ),
    )

    runBlocking { dnsApi.createZone(ZoneRequest("xxx", null)) }

    verify(postRequestedFor(urlMatching("/api/v1/zones")).withRequestBody(notMatching(".*ttl.*")))
  }
}
