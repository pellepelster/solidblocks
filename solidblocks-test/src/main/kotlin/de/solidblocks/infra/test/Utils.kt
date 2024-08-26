package de.solidblocks.infra.test

import java.text.DecimalFormat
import kotlin.time.Duration
import kotlin.time.TimeSource


val durationFormat = DecimalFormat("000.000")

enum class LogType { stdout, stderr, test }

val logTypeMaxLength = LogType.entries.maxOf { it.name.length }

fun log(start: TimeSource.Monotonic.ValueTimeMark, message: String, type: LogType = LogType.test) =
    log(TimeSource.Monotonic.markNow() - start, message, type)

fun log(duration: Duration, message: String, type: LogType = LogType.test) {
    val logType = type.name.padStart(logTypeMaxLength)
    println("${durationFormat.format(duration.inWholeMilliseconds / 1000f)}s [${logType}] ${message}")
}