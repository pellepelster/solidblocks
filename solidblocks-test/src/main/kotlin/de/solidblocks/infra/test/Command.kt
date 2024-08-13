package de.solidblocks.infra.test

import de.solidblocks.infra.test.output.OutputMatcher
import de.solidblocks.infra.test.output.OutputMatcherResult
import de.solidblocks.infra.test.output.waitForOutputMatcher
import de.solidblocks.infra.test.output.waitForOutputMatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
    val unmatchedOutputMatchers: List<OutputMatcherResult>,
)

data class ProcessResult(
    val exitCode: Int,
    val runtime: Duration,
)

class CommandRun(
    private val result: Deferred<ProcessResult>,
    private val unmatchedOutputMatchers: Deferred<List<OutputMatcherResult>>,
    private val output: Queue<OutputLine>,
    private val start: TimeSource.Monotonic.ValueTimeMark,
    private val stdin: Channel<String>,
) {
    fun result() = runBlocking {
        CommandRunResult(
            CommandResult(result.await().exitCode, result.await().runtime, output.toList()),
            unmatchedOutputMatchers.await()
        )
    }

    fun waitForOutput(regex: String) = runBlocking {
        waitForOutputMatcher(start, OutputMatcher(regex.toRegex(), 5.seconds, null), output, stdin)
    }
}

abstract class CommandBuilder(protected var executable: String) {

    protected var timeout: Duration = Int.MAX_VALUE.seconds

    protected val outputMatchers: Queue<OutputMatcher> = LinkedList()

    fun timeout(timeout: Duration) = apply { this.timeout = timeout }

    fun waitForOutput(regex: String, timeout: Duration = 5.seconds, stdin: (() -> String)? = null) = apply {
        this.outputMatchers.add(OutputMatcher(regex.toRegex(), timeout, stdin))
    }

    fun waitForOutputs(outputMatchers: List<OutputMatcher>) = apply {
        this.outputMatchers.addAll(outputMatchers)
    }

    fun runResult() = runBlocking {

        val stdin = Channel<String>()
        val stdout: Queue<OutputLine> = LinkedList()
        val stdoutResult = mutableListOf<OutputLine>()
        val start = TimeSource.Monotonic.markNow()

        val unmatchedWaitForMatchers = async {
            waitForOutputMatchers(start, outputMatchers, stdout, stdin)
        }

        val result = runInternal(start, stdin) {
            stdout.add(it)
            stdoutResult.add(it)

            log(
                start, it.line, when (it.type) {
                    OutputType.stdout -> LogType.stdout
                    OutputType.stderr -> LogType.stderr
                }
            )
        }.await()

        CommandRunResult(
            CommandResult(result.exitCode, result.runtime, stdoutResult),
            unmatchedWaitForMatchers.await()
        )
    }

    suspend fun run() = withContext(Dispatchers.IO) {
        val output: Queue<OutputLine> = LinkedList()
        val output1: Queue<OutputLine> = LinkedList()
        val stdin = Channel<String>()
        val start = TimeSource.Monotonic.markNow()

        val unmatchedOutputMatchers = async {
            waitForOutputMatchers(start, outputMatchers, output, stdin)
        }

        val result = runInternal(start, stdin) {
            output.add(it)
            output1.add(it)
            log(
                start, it.line, when (it.type) {
                    OutputType.stdout -> LogType.stdout
                    OutputType.stderr -> LogType.stderr
                }
            )
        }

        CommandRun(result, unmatchedOutputMatchers, output1, start, stdin)
    }

    abstract suspend fun runInternal(
        start: TimeSource.Monotonic.ValueTimeMark,
        stdin: Channel<String>,
        output: (entry: OutputLine) -> Unit
    ): Deferred<ProcessResult>

}