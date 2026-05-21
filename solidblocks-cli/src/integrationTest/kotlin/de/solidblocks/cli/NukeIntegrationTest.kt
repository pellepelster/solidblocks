package de.solidblocks.cli

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes

@ExtendWith(SolidblocksTest::class)
class NukeIntegrationTest {
    val blcksCommand = Path.of(".").resolve("blcks")

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
    fun testNuke(context: SolidblocksTestContext) {
        val testbed = Path.of(NukeIntegrationTest::class.java.getResource("/nuke-testbed").toURI())

        val terraform = context.terraform(testbed)
        terraform.init()
        terraform.apply()

        assertSoftly(context.local().command("$blcksCommand", "hetzner", "nuke", "--do-nuke").timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()) { result ->
            result shouldHaveExitCode 0
        }
    }
}
