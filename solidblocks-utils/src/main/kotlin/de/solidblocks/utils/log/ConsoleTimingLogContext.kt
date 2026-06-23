package de.solidblocks.utils.log

import de.solidblocks.cloud.api.log.TimingLogContext
import kotlin.time.TimeSource

class ConsoleTimingLogContext(override val start: TimeSource.Monotonic.ValueTimeMark, indent: Int = 0) : TimingLogContext, ConsoleLogContext(indent)
