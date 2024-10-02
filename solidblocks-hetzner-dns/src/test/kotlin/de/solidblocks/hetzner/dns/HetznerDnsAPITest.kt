package de.solidblocks.hetzner.dns

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.notMatching
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import de.solidblocks.hetzner.dns.model.ZoneRequest
import org.junit.jupiter.api.Test
import java.time.Instant

@WireMockTest
class HetznerDnsAPITest {

    @Test
    fun testConnectFailure() {
        val dnsApi = HetznerDnsApi("api-token", "http://localhost:1234/api/v1")

        assertSoftly(dnsApi.zoneById("1234")) {
            it.isFailure shouldBe true
            it.exceptionOrNull()?.message shouldStartWith "Failed to connect to localhost"
        }
    }

    @Test
    fun testApiTokenIsProvided(wiremock: WireMockRuntimeInfo) {
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
                        """.trimIndent()
                    )
                )
        )

        val api = HetznerDnsApi("api-token", "http://localhost:${wiremock.httpPort}/api/v1")
        assertSoftly(api.zoneById("1234")) {
            it.isSuccess shouldBe true
            it.getOrNull()?.zone?.id shouldBe "1234"
            it.getOrNull()?.zone?.created shouldBe Instant.parse("2022-09-03T18:38:52.597Z")
        }

        verify(
            getRequestedFor(urlMatching("/api/v1/zones/1234"))
                .withHeader("Auth-API-Token", matching("api-token"))
        )
    }

    @Test
    fun testUnknownFieldsAreIgnored(wiremock: WireMockRuntimeInfo) {
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
                        """.trimIndent()
                    )
                )
        )

        val api = HetznerDnsApi("api-token", "http://localhost:${wiremock.httpPort}/api/v1")
        assertSoftly(api.zoneById("1234")) {
            it.isSuccess shouldBe true
            it.getOrNull()?.zone?.id shouldBe "1234"
            it.getOrNull()?.zone?.created shouldBe Instant.parse("2022-09-03T18:38:52.597Z")
        }

        verify(
            getRequestedFor(urlMatching("/api/v1/zones/1234"))
                .withHeader("Auth-API-Token", matching("api-token"))
        )
    }

    @Test
    fun testNotFound(wiremock: WireMockRuntimeInfo) {
        stubFor(
            get(urlEqualTo("/api/v1/zones/1234"))
                .willReturn(notFound())
        )

        val api = HetznerDnsApi("api-token", "http://localhost:${wiremock.httpPort}/api/v1")
        assertSoftly(api.zoneById("1234")) {
            it.isFailure shouldBe true
        }

        verify(
            getRequestedFor(urlMatching("/api/v1/zones/1234"))
                .withHeader("Auth-API-Token", matching("api-token"))
        )
    }

    @Test
    fun testNullFieldsAreNotSent(wiremock: WireMockRuntimeInfo) {
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
                        """.trimIndent()
                    )
                )
        )

        val api = HetznerDnsApi("api-token", "http://localhost:${wiremock.httpPort}/api/v1")
        api.createZone(ZoneRequest("xxx", null))
        verify(postRequestedFor(urlMatching("/api/v1/zones")).withRequestBody(notMatching(".*ttl.*")))
    }
}
