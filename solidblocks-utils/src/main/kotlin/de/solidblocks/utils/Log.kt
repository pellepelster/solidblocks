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

fun logInfoBlcks(message: String, source: LogSource? = LogSource.BLCKS, duration: Duration? = null) =
    log(message, LogLevel.INFO, source, duration)

fun logInfoBlcks(message: String, source: LogSource? = LogSource.BLCKS, start: ValueTimeMark?) =
    log(message, LogLevel.INFO, source, start?.let { TimeSource.Monotonic.markNow() - it })

fun logInfo(message: String, source: LogSource? = null, duration: Duration? = null) =
    log(message, LogLevel.INFO, source, duration)


fun logInfo(message: String, source: LogSource? = null, start: ValueTimeMark?) =
    log(message, LogLevel.INFO, source, start?.let { TimeSource.Monotonic.markNow() - it })

fun logErrorBlcks(message: String, source: LogSource? = LogSource.BLCKS, duration: Duration? = null) =
    log(message, LogLevel.ERROR, source, duration)

fun logError(message: String, source: LogSource? = null, duration: Duration? = null) =
    log(message, LogLevel.ERROR, source, duration)

fun logSuccessBlcks(message: String, source: LogSource = LogSource.BLCKS, duration: Duration? = null) =
    log(message, LogLevel.SUCCESS, source, duration)

fun logSuccess(message: String, source: LogSource? = null, duration: Duration? = null) =
    log(message, LogLevel.SUCCESS, source, duration)

fun logWarningBlcks(message: String, source: LogSource = LogSource.BLCKS, duration: Duration? = null) =
    log(message, LogLevel.WARNING, source, duration)

fun logWarning(message: String, source: LogSource? = null, duration: Duration? = null) =
    log(message, LogLevel.WARNING, source, duration)

fun logDebugBlcks(message: String, source: LogSource = LogSource.BLCKS, duration: Duration? = null) =
    log(message, LogLevel.DEBUG, source, duration)

fun logDebug(message: String, source: LogSource? = null, duration: Duration? = null) =
    log(message, LogLevel.DEBUG, source, duration)

fun log(message: String, level: LogLevel, source: LogSource?, duration: Duration? = null) {
    val color: COLORS? = when (level) {
        LogLevel.INFO -> null
        LogLevel.DEBUG -> COLORS.BRIGHT_BLUE
        LogLevel.ERROR -> COLORS.RED
        LogLevel.SUCCESS -> COLORS.GREEN
        LogLevel.WARNING -> COLORS.YELLOW
    }

    val formattedMessage = if (color == null) {
        message
    } else {
        color(message, color)
    }

    val formattedSource = if (source == null) {
        null
    } else {
        "[${source}]"
    }

    val formattedDuration = if (duration == null) {
        null
    } else {
        "[${durationFormatter.format(duration.inWholeMilliseconds / 1000f)}s]"
    }

    val formattedParts = listOf(formattedSource, formattedDuration, formattedMessage).filterNotNull().joinToString(" ")

    println("[${level.name.padStart(logLevelMaxLength)}] ${formattedParts}")
}

