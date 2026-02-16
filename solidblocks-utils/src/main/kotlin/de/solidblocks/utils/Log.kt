package de.solidblocks.utils

import de.solidblocks.utils.Constants.durationFormatter
import java.text.DecimalFormat
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

object Constants {
  val durationFormatter = DecimalFormat("000.000")
}

public enum class LogSource {
  BLCKS,
  STDOUT,
  STDERR,
  CLOUDINIT,
}

public enum class LogLevel {
  INFO,
  ERROR,
  SUCCESS,
  WARNING,
  DEBUG,
}

val logLevelMaxLength = LogLevel.entries.maxOf { it.name.length }

fun logInfoBlcks(
    message: String,
    source: LogSource? = LogSource.BLCKS,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.INFO, source, duration, context)

fun logInfoBlcks(
    message: String,
    source: LogSource? = LogSource.BLCKS,
    start: ValueTimeMark?,
    context: LogContext? = null,
) = log(message, LogLevel.INFO, source, start?.let { TimeSource.Monotonic.markNow() - it }, context)

fun logInfo(
    message: String,
    source: LogSource? = null,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.INFO, source, duration, context)

fun logInfo(message: String, source: LogSource? = null, context: LogContext? = null) =
    log(message, LogLevel.INFO, source, null, context)

fun logErrorBlcks(
    message: String,
    source: LogSource? = LogSource.BLCKS,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.ERROR, source, duration, context)

fun logError(
    message: String,
    source: LogSource? = null,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.ERROR, source, duration, context)

fun logSuccessBlcks(
    message: String,
    source: LogSource = LogSource.BLCKS,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.SUCCESS, source, duration, context)

fun logSuccess(
    message: String,
    source: LogSource? = null,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.SUCCESS, source, duration, context)

fun logWarningBlcks(
    message: String,
    source: LogSource = LogSource.BLCKS,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.WARNING, source, duration, context)

fun logWarning(
    message: String,
    source: LogSource? = null,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.WARNING, source, duration, context)

fun logDebugBlcks(
    message: String,
    source: LogSource = LogSource.BLCKS,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.DEBUG, source, duration, context)

fun logDebug(
    message: String,
    source: LogSource? = null,
    duration: Duration? = null,
    context: LogContext? = null,
) = log(message, LogLevel.DEBUG, source, duration, context)

fun log(
    message: String,
    level: LogLevel,
    source: LogSource?,
    duration: Duration? = null,
    context: LogContext? = null,
) {
  val color: COLORS? =
      when (level) {
        LogLevel.INFO -> null
        LogLevel.DEBUG -> COLORS.BRIGHT_BLUE
        LogLevel.ERROR -> COLORS.RED
        LogLevel.SUCCESS -> COLORS.GREEN
        LogLevel.WARNING -> COLORS.YELLOW
      }

  val formattedMessage =
      if (color == null) {
        message
      } else {
        color(message, color)
      }

  val formattedSource =
      if (source == null) {
        null
      } else {
        "[$source]"
      }

  val d =
      duration
          ?: when (context) {
            is TimingLogContext -> TimeSource.Monotonic.markNow() - context.start
            else -> null
          }

  val formattedDuration =
      if (d == null) {
        null
      } else {
        "[${durationFormatter.format(d.inWholeMilliseconds / 1000f)}s]"
      }

  val formattedParts =
      listOfNotNull(formattedSource, formattedDuration, formattedMessage).joinToString(" ")

  println(
      "[${level.name.padStart(logLevelMaxLength)}] ${"  ".repeat(context?.indent ?: 0)}$formattedParts",
  )
}
