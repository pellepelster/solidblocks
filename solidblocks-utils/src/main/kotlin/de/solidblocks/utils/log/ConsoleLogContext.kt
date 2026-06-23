package de.solidblocks.utils.log

import de.solidblocks.cloud.api.log.LogContext
import de.solidblocks.cloud.api.log.TimingLogContext
import de.solidblocks.utils.logDebug
import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logSuccess
import de.solidblocks.utils.logWarning
import kotlin.time.TimeSource

open class ConsoleLogContext(override val indent: Int = 0) : LogContext {
    override fun indent(): LogContext = ConsoleLogContext(indent + 1)

    override fun unindent(): LogContext = ConsoleLogContext(indent - 1)

    override fun withTiming(): TimingLogContext = ConsoleTimingLogContext(TimeSource.Monotonic.markNow(), indent)

    override fun info(message: String) = logInfo(message, context = this)

    override fun warning(message: String) = logWarning(message, context = this)

    override fun debug(message: String) = logDebug(message, context = this)

    override fun error(message: String) = logError(message, context = this)

    override fun success(message: String) = logSuccess(message, context = this)

    override fun bold(message: String) = de.solidblocks.utils.bold(message)

    override fun dim(message: String) = de.solidblocks.utils.dim(message)

    companion object {
        fun default() = ConsoleLogContext(0)

        fun withTiming() = ConsoleTimingLogContext(TimeSource.Monotonic.markNow())
    }
}
