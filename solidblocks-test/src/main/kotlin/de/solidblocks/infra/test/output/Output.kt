package de.solidblocks.infra.test.output

import de.solidblocks.infra.test.OutputLine
import de.solidblocks.infra.test.log
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.util.*
import kotlin.time.Duration
import kotlin.time.TimeSource

data class OutputMatcher(val regex: Regex, val timeout: Duration, val stdin: (() -> String)?)

data class OutputMatcherResult(val regex: Regex, val matched: Boolean)

suspend fun waitForOutputMatchers(
    start: TimeSource.Monotonic.ValueTimeMark,
    outputMatchers: Queue<OutputMatcher>,
    output: Queue<OutputLine>,
    stdin: SendChannel<String>
) = outputMatchers.map { waitForOutputMatcher(start, it, output, stdin) }.filter { !it.matched }

suspend fun waitForOutputMatcher(
    start: TimeSource.Monotonic.ValueTimeMark,
    outputMatcher: OutputMatcher,
    output: Queue<OutputLine>,
    stdin: SendChannel<String>
): OutputMatcherResult {

    log(
        TimeSource.Monotonic.markNow() - start,
        "waiting for log line '${outputMatcher.regex}' with a timeout of ${outputMatcher.timeout}"
    )

    return try {
        withTimeout(outputMatcher.timeout) {
            var matched = false

            while (!matched) {
                if (!output.isEmpty()) {
                    val entry = output.remove()
                    matched = entry.line.matches(outputMatcher.regex)
                    if (matched) {
                        outputMatcher.stdin?.invoke()?.let {
                            stdin.send(it)
                        }
                    }
                }
                yield()
            }

            OutputMatcherResult(outputMatcher.regex, true)
        }
    } catch (e: TimeoutCancellationException) {
        log(
            TimeSource.Monotonic.markNow() - start,
            "timeout of ${outputMatcher.timeout} exceeded waiting for log line '${outputMatcher.regex}'"
        )

        OutputMatcherResult(outputMatcher.regex, false)
    }
}
