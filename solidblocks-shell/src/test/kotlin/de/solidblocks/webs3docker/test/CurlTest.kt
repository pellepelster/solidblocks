package de.solidblocks.webs3docker.test

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import localTestContext
import org.junit.jupiter.api.Test
import java.util.*

@WireMockTest
public class CurlTest {
    @Test
    fun testRetriesDownload(wmRuntimeInfo: WireMockRuntimeInfo) {
        val reponse = UUID.randomUUID().toString()

        stubFor(
            get("/download-test")
                .inScenario("curl_retry")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("download_redirect")
                .willReturn(aResponse().withStatus(500)),
        )

        stubFor(
            get("/download-test")
                .inScenario("curl_retry")
                .whenScenarioStateIs("download_redirect")
                .willSetStateTo("download_success")
                .willReturn(
                    aResponse().withHeader("Location", "/download-test-success").withStatus(301),
                ),
        )

        stubFor(
            get("/download-test-success")
                .inScenario("curl_retry")
                .whenScenarioStateIs("download_success")
                .willReturn(aResponse().withBody("${reponse}\n").withStatus(200)),
        )

        val result =
            localTestContext()
                .script()
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("curl.sh"))
                .step(
                    "echo response=$(curl_wrapper \"http://localhost:${wmRuntimeInfo.httpPort}/download-test\")",
                )
                .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*response=$reponse.*"
        }
    }
}
