package de.solidblocks.infra.test

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
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
    val unmatchedWaitForOutput: List<WaitForOutput>,
    val result: CommandResult,

    )

data class CommandRun(
    val start: TimeSource.Monotonic.ValueTimeMark,
    val result: CommandResult,
)

data class WaitForOutput(val regex: Regex, val timeout: Duration)

abstract class CommandBuilder(protected var executable: String) {

    protected var timeout: Duration = Int.MAX_VALUE.seconds

    protected val waitForOutput: Queue<WaitForOutput> = LinkedList()

    fun timeout(timeout: Duration) = apply { this.timeout = timeout }

    fun waitForOutput(regex: String, timeout: Duration = 5.seconds) = apply {
        this.waitForOutput.add(WaitForOutput(regex.toRegex(), timeout))
    }

    fun run() = runInternal()

    protected suspend fun waitForOutput1(
        start: TimeSource.Monotonic.ValueTimeMark,
        waitForOutput: Queue<WaitForOutput>,
        output: Queue<OutputLine>
    ): MutableList<WaitForOutput> {

        val unmatchedWaitForOutput = mutableListOf<WaitForOutput>()

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
                        }
                        yield()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                log(
                    TimeSource.Monotonic.markNow() - start,
                    "timeout exceeded waiting for log line '${waitFor.regex}' (${waitFor.timeout})"
                )
                unmatchedWaitForOutput.add(waitFor)
            }
        }

        return unmatchedWaitForOutput
    }

    abstract fun runInternal(): CommandRunResult

}