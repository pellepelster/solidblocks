package de.solidblocks.utils

import kotlin.time.TimeSource

open class LogContext(val indent: Int = 0) {
    fun indent(): LogContext = LogContext(indent + 1)

    fun unindent(): LogContext = LogContext(indent - 1)

    fun withTiming(): LogContext = TimingLogContext(TimeSource.Monotonic.markNow(), indent)

    fun info(message: String) = logInfo(message, context = this)

    fun warning(message: String) = logWarning(message, context = this)

    fun debug(message: String) = logDebug(message, context = this)

    fun error(message: String) = logError(message, context = this)

    companion object {
        fun default() = LogContext(0)

        fun withTiming() = TimingLogContext(TimeSource.Monotonic.markNow())
    }
}

class TimingLogContext(val start: TimeSource.Monotonic.ValueTimeMark, indent: Int = 0) : LogContext(indent)
