package de.solidblocks.utils.log

import kotlin.time.TimeSource

class ConsoleTimingLogContext(override val start: TimeSource.Monotonic.ValueTimeMark, indent: Int = 0) : TimingLogContext, ConsoleLogContext(indent)
