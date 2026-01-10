package de.solidblocks.infra.test.output

import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logInfo
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

enum class OutputType {
  STDOUT,
  STDERR,
}

open class OutputLine(val line: String, val type: OutputType)

class TimestampedOutputLine(
    val timestamp: TimeSource.Monotonic.ValueTimeMark,
    line: String,
    type: OutputType,
) : OutputLine(line, type)

data class OutputMatcher(val regex: Regex, val timeout: Duration, val answer: (() -> String)?)

data class OutputMatcherResult(val regex: Regex, val matched: Boolean)

suspend fun waitForOutputMatcher(
    context: LogContext,
    outputMatcher: OutputMatcher,
    output: List<TimestampedOutputLine>,
    stdin: SendChannel<String>,
) {
  logInfo(
      "waiting for log line '${outputMatcher.regex}' with a timeout of ${outputMatcher.timeout}",
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
            outputMatcher.answer?.invoke()?.let { stdin.send(it) }
          }
        }

        yield()
      }
    }
  } catch (e: TimeoutCancellationException) {
    val message =
        "timeout of ${outputMatcher.timeout} exceeded waiting for log line '${outputMatcher.regex}'"
    logInfo(
        message,
        context = context,
    )

    throw RuntimeException(message)
  }
}
