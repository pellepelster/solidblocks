package de.solidblocks.infra.test

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
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
    val unmatchedWaitForMatchers: List<WaitForOutput>,
)

data class ProcessResult(
    val exitCode: Int,
    val runtime: Duration,
)

/*
class CommandRun(
    private val result: Deferred<CommandResult>,
) {
    fun result() = runBlocking {
        CommandRunResult(result.await(), unmatchedWaitForMatchers)
    }

    fun waitForOutput(waitForOutput: String) {

    }
}
*/

data class WaitForOutput(val regex: Regex, val timeout: Duration, val stdin: (() -> String)?)

abstract class CommandBuilder(protected var executable: String) {

    protected var timeout: Duration = Int.MAX_VALUE.seconds

    protected val waitForOutput: Queue<WaitForOutput> = LinkedList()

    fun timeout(timeout: Duration) = apply { this.timeout = timeout }

    fun waitForOutput(regex: String, timeout: Duration = 5.seconds, stdin: (() -> String)? = null) = apply {
        this.waitForOutput.add(WaitForOutput(regex.toRegex(), timeout, stdin))
    }

    fun runResult() = runBlocking {

        val output: Queue<OutputLine> = LinkedList()
        val stdin = Channel<String>()
        val start = TimeSource.Monotonic.markNow()

        val unmatchedWaitForMatchers = async {
            waitForOutput(start, waitForOutput, output, stdin)
        }

        val result = runInternal(start, stdin) {
            output.add(it)

            log(
                start, it.line, when (it.type) {
                    OutputType.stdout -> LogType.stdout
                    OutputType.stderr -> LogType.stderr
                }
            )
        }.await()

        CommandRunResult(
            CommandResult(result.exitCode, result.runtime, output.toList()),
            unmatchedWaitForMatchers.await()
        )
    }

    //suspend fun run() = runInternal()

    protected suspend fun waitForOutput(
        start: TimeSource.Monotonic.ValueTimeMark,
        waitForOutput: Queue<WaitForOutput>,
        output: Queue<OutputLine>,
        stdin: SendChannel<String>
    ): List<WaitForOutput> {

        val unmatchedWaitForOutputs = mutableListOf<WaitForOutput>()

        while (waitForOutput.isNotEmpty()) {
            val waitFor = waitForOutput.remove()

            log(
                TimeSource.Monotonic.markNow() - start,
                "waiting for log line '${waitFor.regex}' with a timeout of ${waitFor.timeout}"
            )

            try {
                withTimeout(waitFor.timeout) {
                    var matched = false

                    while (!matched) {
                        if (!output.isEmpty()) {
                            val entry = output.remove()
                            matched = entry.line.matches(waitFor.regex)
                            if (matched) {
                                waitFor.stdin?.invoke()?.let {
                                    stdin.send(it)
                                }
                            }
                        }
                        yield()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                log(
                    TimeSource.Monotonic.markNow() - start,
                    "timeout of ${waitFor.timeout} exceeded waiting for log line '${waitFor.regex}'"
                )
                unmatchedWaitForOutputs.add(waitFor)
            }
        }

        return unmatchedWaitForOutputs
    }

    abstract suspend fun runInternal(
        start: TimeSource.Monotonic.ValueTimeMark,
        stdin: Channel<String>,
        output: (entry: OutputLine) -> Unit
    ): Deferred<ProcessResult>

}