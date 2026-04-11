package de.solidblocks.cli

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

@ExtendWith(SolidblocksTest::class)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
class IntegrationTest {
    val blcksCommand = Path.of(".").resolve("blcks")

    @Test
    fun testBinaryIsValid(context: SolidblocksTestContext) {
        assertSoftly(context.local().command(blcksCommand).runResult()) { result ->
            result shouldHaveExitCode 0
            listOf("cloud", "hetzner", "github", "docs", "terraform", "tofu").forEach {
                result stdoutShouldMatch ".*${it}.*"
            }
        }
    }

    @Test
    fun testNuke(context: SolidblocksTestContext) {
        assertSoftly(context.local().command("${blcksCommand}", "hetzner", "nuke", "--do-nuke").timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()) { result ->
            result shouldHaveExitCode 0
        }
    }
}