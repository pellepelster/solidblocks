package de.solidblocks.cli

import com.jayway.jsonpath.JsonPath
import de.solidblocks.cloud.DnsService
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stderrShouldContain
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.utils.logInfo
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@ExtendWith(SolidblocksTest::class)
class NukeIntegrationTest {
    val blcksCommand = Path.of(".").resolve("blcks")

    @Test
    fun testNuke(context: SolidblocksTestContext) {
        val testbed = NukeIntegrationTest::class.java.getResource("/nuke-testbed").path
        val terraform = context.terraform(testbed)
        terraform.init()
        terraform.apply()

        assertSoftly(context.local().command("$blcksCommand", "hetzner", "nuke", "--do-nuke").timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()) { result ->
            result shouldHaveExitCode 0
        }
    }
}
