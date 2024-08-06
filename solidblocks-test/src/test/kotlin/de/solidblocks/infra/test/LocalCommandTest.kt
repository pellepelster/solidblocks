import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

public class LocalCommandTest {

    @Test
    fun testFailure() {
        val executable = this.javaClass.classLoader.getResource("command-failure.sh")!!.path
        assertSoftly(localCommand(executable).run()) {
            it shouldHaveExitCode 1
        }
    }

    @Test
    fun testExitCodeAssertion() {
        val executable = this.javaClass.classLoader.getResource("command-failure.sh")!!.path
        shouldThrow<AssertionError> {
            assertSoftly(localCommand(executable).run()) {
                it shouldHaveExitCode 99
            }
        }
    }

    @Test
    fun testSuccess() {
        val executable = this.javaClass.classLoader.getResource("command-success.sh")!!.path
        assertSoftly(localCommand(executable).run()) {
            it shouldHaveExitCode 0
        }
    }

    @Test
    fun testStdout() {
        val executable = this.javaClass.classLoader.getResource("command-stdout.sh")!!.path
        assertSoftly(localCommand(executable).run()) {
            it shouldHaveExitCode 0
        }
    }

    @Test
    fun testStderr() {
        val executable = this.javaClass.classLoader.getResource("command-stderr.sh")!!.path
        assertSoftly(localCommand(executable).run()) {
            it shouldHaveExitCode 0
            it.result.stderr shouldBe "stderr line 1\nstderr line 2\nstderr line 3"
        }
    }

    @Test
    fun testTimeoutExceeded() {
        val executable = this.javaClass.classLoader.getResource("command-timeout.sh")!!.path

        measureTime {
            assertSoftly(localCommand(executable).timeout(2.seconds).run()) {
                it shouldHaveExitCode 137
                it runtimeShouldBeLessThan 3.seconds
                it runtimeShouldBeGreaterThan 1.milliseconds
            }
        } shouldBeLessThan 3.seconds


    }

    @Test
    fun testWaitForLogLineMatched() {
        val executable = this.javaClass.classLoader.getResource("command-waitfor.sh")!!.path

        assertSoftly(
            localCommand(executable)
                .waitForLogLine(".*marker 1.*".toRegex())
                .waitForLogLine(".*marker 2.*".toRegex())
                .waitForLogLine(".*marker 3.*".toRegex()).run()
        ) {
            it.shouldNotHaveUnmatchedWaitForLogLines()
        }

    }

    @Test
    fun testWaitForLogLineNotMatched() {
        val executable = this.javaClass.classLoader.getResource("command-waitfor.sh")!!.path

        assertSoftly(
            localCommand(executable)
                .waitForLogLine("yolo".toRegex()).run()
        ) {
            it shouldHaveExitCode 0
            it shouldHaveUnmatchedWaitForLogLines 1
        }
    }
}