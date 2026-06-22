package de.solidblocks.utils.log

import kotlin.time.TimeSource

interface LogContext {
    val indent: Int

    fun indent(): LogContext
    fun unindent(): LogContext

    fun withTiming(): TimingLogContext

    fun info(message: String)
    fun warning(message: String)
    fun debug(message: String)
    fun error(message: String)
    fun success(message: String)
}

interface TimingLogContext : LogContext {
    val start: TimeSource.Monotonic.ValueTimeMark
}
