package solidblocks.test.gradle.command

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ExtendWith(SolidblocksTest::class)
class CommandAssertions {
    @Test
    fun commandAssertions(testContext: SolidblocksTestContext) {
        val result = testContext.local().command("whoami").runResult()

        result shouldHaveExitCode 0

        result outputShouldBe "something"
        result stderrShouldBe "something"

        result stderrShouldMatch ".*something.*"
        result stdoutShouldMatch ".*something.*"

        result.stdoutShouldBeEmpty()
        result.stderrShouldBeEmpty()

        result runtimeShouldBeGreaterThan 10.milliseconds
        result runtimeShouldBeLessThan 5.seconds
    }
}
