import de.solidblocks.infra.test.*
import de.solidblocks.infra.test.docker.docker
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeLessThan
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

public class CommandTest {

    private fun getCommandPath(path: String) = Path(this.javaClass.classLoader.getResource(path)!!.path)

    companion object {
        val contexts = listOf(local(), docker())
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testFailure(context: TestContext) {
        assertSoftly(context.command(getCommandPath("command-failure.sh")).runResult()) {
            it shouldHaveExitCode 1
        }
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testRunResult(context: TestContext) {
        assertSoftly(context.command(getCommandPath("command-failure.sh")).runResult()) {
            it shouldHaveExitCode 1
        }
    }

    /*
    @ParameterizedTest
    @FieldSource("contexts")
    fun testRunAndResult(context: TestContext) {
        runBlocking {
            val run = context.command(getCommandPath("command-failure.sh")).run()

            run.waitForOutput(".*marker 1.*")

            assertSoftly(run.result()) {
                it shouldHaveExitCode 1
            }
        }
    }
    */

    @ParameterizedTest
    @FieldSource("contexts")
    fun testExitCodeAssertionNegative(context: TestContext) {
        shouldThrow<AssertionError> {
            assertSoftly(context.command(getCommandPath("command-failure.sh")).runResult()) {
                it shouldHaveExitCode 99
            }
        }
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testSuccess(context: TestContext) {
        assertSoftly(context.command(getCommandPath("command-success.sh")).runResult()) {
            it shouldHaveExitCode 0
        }
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testStdout(context: TestContext) {
        assertSoftly(context.command(getCommandPath("command-stdout.sh")).runResult()) {
            it shouldHaveExitCode 0
            it stdoutShouldBe "stdout line 1\nstdout line 2\nstdout line 3\n"
            it stderrShouldBe ""
        }
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testStderr(context: TestContext) {
        assertSoftly(context.command(getCommandPath("command-stderr.sh")).runResult()) {
            it shouldHaveExitCode 0
            it stdoutShouldBe ""
            it stderrShouldBe "stderr line 1\nstderr line 2\nstderr line 3\n"
        }
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testOutput(context: TestContext) {
        assertSoftly(
            context.command(getCommandPath("command-output.sh")).runResult()
        ) {
            it shouldHaveExitCode 0

            it outputShouldBe "stderr line 1\nstdout line 1\nstderr line 2\nstdout line 2\nstderr line 3\nstdout line 3\n"
            it outputShouldMatch ".*stderr line 1.*"
            it outputShouldMatch ".*stderr line 2.*"
            it outputShouldMatch ".*stderr line 3.*"
            it outputShouldMatch ".*stdout line 1.*"
            it outputShouldMatch ".*stdout line 2.*"
            it outputShouldMatch ".*stdout line 3.*"

            it stderrShouldBe "stderr line 1\nstderr line 2\nstderr line 3\n"
            it stderrShouldMatch ".*stderr line 1.*"
            it stderrShouldMatch ".*stderr line 2.*"
            it stderrShouldMatch ".*stderr line 3.*"

            it stdoutShouldBe "stdout line 1\nstdout line 2\nstdout line 3\n"
            it stdoutShouldMatch ".*stdout line 1.*"
            it stdoutShouldMatch ".*stdout line 2.*"
            it stdoutShouldMatch ".*stdout line 3.*"
        }
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testTimeoutExceeded(context: TestContext) {
        measureTime {
            assertSoftly(context.command(getCommandPath("command-timeout.sh")).timeout(2.seconds).runResult()) {
                it shouldHaveExitCode 137
                it runtimeShouldBeLessThan 3.seconds + 500.milliseconds
                it runtimeShouldBeGreaterThan 1.seconds
            }
        } shouldBeLessThan 4.seconds
    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testWaitForLogLineMatched(context: TestContext) {
        assertSoftly(
            context.command(getCommandPath("command-waitfor.sh"))
                .waitForOutput(".*marker 1.*")
                .waitForOutput(".*marker 2.*")
                .waitForOutput(".*marker 3.*").runResult()
        ) {
            it.shouldNotHaveUnmatchedWaitForOutput()
        }

    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testStdIn(context: TestContext) {
        assertSoftly(
            context.command(getCommandPath("command-stdin.sh"))
                .waitForOutput(".*marker 1.*") { "a" }
                .waitForOutput(".*marker 2.*") { "b" }
                .waitForOutput(".*marker 3.*") { "c" }.runResult()
        ) {
            it.shouldNotHaveUnmatchedWaitForOutput()
        }

    }

    @ParameterizedTest
    @FieldSource("contexts")
    fun testWaitForLogLineNotMatched(context: TestContext) {
        assertSoftly(
            context.command(getCommandPath("command-waitfor.sh"))
                .waitForOutput("yolo").runResult()
        ) {
            it shouldHaveExitCode 0
            it shouldHaveUnmatchedWaitForOutput 1
        }
    }

}
