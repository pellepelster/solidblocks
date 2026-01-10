package de.solidblocks.utils

import kotlin.time.TimeSource

open class LogContext(val indent: Int = 0) {
    fun indent(): LogContext = LogContext(indent + 1)
    fun unindent(): LogContext = LogContext(indent - 1)

    companion object {
        fun default() = LogContext(0)
        fun withTiming() = TimingLogContext(TimeSource.Monotonic.markNow())
    }
}

class TimingLogContext(val start: TimeSource.Monotonic.ValueTimeMark) : LogContext(0)