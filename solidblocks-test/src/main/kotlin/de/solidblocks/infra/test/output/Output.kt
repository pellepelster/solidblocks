package de.solidblocks.infra.test.output

import de.solidblocks.infra.test.log
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.time.Duration
import kotlin.time.TimeSource

enum class OutputType { stdout, stderr }

data class OutputLine(val timestamp: Duration, val line: String, val type: OutputType)

data class OutputMatcher(val regex: Regex, val timeout: Duration, val answer: (() -> String)?)

data class OutputMatcherResult(val regex: Regex, val matched: Boolean)

suspend fun waitForOutputMatcher(
    start: TimeSource.Monotonic.ValueTimeMark,
    outputMatcher: OutputMatcher,
    output: List<OutputLine>,
    stdin: SendChannel<String>
) {

    log(
        TimeSource.Monotonic.markNow() - start,
        "waiting for log line '${outputMatcher.regex}' with a timeout of ${outputMatcher.timeout}"
    )

    try {
        withTimeout(outputMatcher.timeout) {
            var matched = false
            var index = -1

            while (!matched) {
                if (output.size > 0 && output.lastIndex > index) {
                    index++
                    val entry = output[index]
                    matched = entry.line.matches(outputMatcher.regex)
                    if (matched) {
                        outputMatcher.answer?.invoke()?.let {
                            stdin.send(it)
                        }
                    }
                }

                yield()
            }
        }
    } catch (e: TimeoutCancellationException) {
        val message = "timeout of ${outputMatcher.timeout} exceeded waiting for log line '${outputMatcher.regex}'"
        log(
            TimeSource.Monotonic.markNow() - start,
            message
        )

        throw RuntimeException(message)
    }
}
