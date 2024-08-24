package de.solidblocks.infra.test

import de.solidblocks.infra.test.CommandBuilder.Companion.waitForOutputDefaultTimeout
import de.solidblocks.infra.test.output.OutputMatcher
import de.solidblocks.infra.test.output.waitForOutputMatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

enum class OutputType { stdout, stderr }

data class OutputLine(val timestamp: Duration, val line: String, val type: OutputType)

data class CommandResult(
    val exitCode: Int,
    val runtime: Duration,
    private val internalOutput: List<OutputLine>
) {
    val stdout: String
        get() = internalOutput.filter { it.type == OutputType.stdout }.joinToString("") { "${it.line}\n" }

    val stderr: String
        get() = internalOutput.filter { it.type == OutputType.stderr }.joinToString("") { "${it.line}\n" }

    val output: String
        get() = internalOutput.joinToString("") { "${it.line}\n" }

}

data class CommandRunResult(
    val result: CommandResult,
)

data class ProcessResult(
    val exitCode: Int,
    val runtime: Duration,
)

class CommandRunAssertion(
    private val start: TimeSource.Monotonic.ValueTimeMark,
    private val stdin: Channel<String>,
    private val output: List<OutputLine>,
) {
    fun waitForOutput(regex: String, timeout: Duration = waitForOutputDefaultTimeout, answer: (() -> String)? = null) = runBlocking {
        waitForOutputMatcher(start, OutputMatcher(regex.toRegex(), timeout, answer), output, stdin)
    }
}


class CommandRun(
    private val start: TimeSource.Monotonic.ValueTimeMark,
    private val stdin: Channel<String>,
    private val result: Deferred<ProcessResult>,
    private val output: List<OutputLine>,
    private val assertionsResult: Deferred<List<Unit>>,
) {
    fun result() = runBlocking {
        assertionsResult.await()

        CommandRunResult(
            CommandResult(result.await().exitCode, result.await().runtime, output),
        )
    }

    fun waitForOutput(regex: String, timeout: Duration = waitForOutputDefaultTimeout, answer: (() -> String)? = null) = runBlocking {
        waitForOutputMatcher(start, OutputMatcher(regex.toRegex(), timeout, answer), output, stdin)
    }

}

abstract class CommandBuilder(protected var executable: String) {

    protected var timeout: Duration = 60.seconds

    companion object {
        val waitForOutputDefaultTimeout: Duration = 60.seconds
    }

    protected var workingDir: Path? = null

    protected val assertions: Queue<(CommandRunAssertion) -> Unit> = LinkedList()

    fun workingDir(workingDir: Path) = apply { this.workingDir = workingDir }

    fun timeout(timeout: Duration) = apply { this.timeout = timeout }

    fun runResult() = runBlocking {
        run().result()
    }

    suspend fun run() = withContext(Dispatchers.IO) {
        val output = mutableListOf<OutputLine>()
        val stdin = Channel<String>()
        val start = TimeSource.Monotonic.markNow()

        val assertionsResult = async {
            assertions.map { it.invoke(CommandRunAssertion(start, stdin, output)) }
        }

        val result = runInternal(start, stdin) {
            output.add(it)

            log(
                start, it.line, when (it.type) {
                    OutputType.stdout -> LogType.stdout
                    OutputType.stderr -> LogType.stderr
                }
            )
        }

        CommandRun(start, stdin, result, output, assertionsResult)
    }

    abstract suspend fun runInternal(
        start: TimeSource.Monotonic.ValueTimeMark,
        stdin: Channel<String>,
        output: (entry: OutputLine) -> Unit
    ): Deferred<ProcessResult>

    fun assert(assertion: (CommandRunAssertion) -> Unit) = apply {
        this.assertions.add(assertion)
    }

}