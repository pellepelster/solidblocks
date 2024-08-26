package de.solidblocks.shell.test

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import de.solidblocks.infra.test.output.stdoutShouldMatch
import de.solidblocks.infra.test.script.script
import de.solidblocks.infra.test.shouldHaveExitCode
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import java.util.UUID

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
                .willReturn(aResponse().withStatus(500))
        )

        stubFor(
            get("/download-test")
                .inScenario("curl_retry")
                .whenScenarioStateIs("download_redirect")
                .willSetStateTo("download_success")
                .willReturn(aResponse().withHeader("Location", "/download-test-success").withStatus(301))
        )

        stubFor(
            get("/download-test-success")
                .inScenario("curl_retry")
                .whenScenarioStateIs("download_success")
                .willReturn(aResponse().withBody("${reponse}\n").withStatus(200))
        )

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("curl.sh"))
            .step("echo response=$(curl_wrapper \"http://localhost:${wmRuntimeInfo.httpPort}/download-test\")")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*response=${reponse}.*"
        }
    }

}
