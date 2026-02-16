package de.solidblocks.utils

import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

class LogTest {

  @Test
  fun testLogFormats() {
    logInfo("a normal message")
    logInfo(bold("some bold message"))
    logInfo(dim("some dim message"))
    logInfo(bold(underline("some bold underlined message")))
    logInfo(strikethrough("some strikethrough message"))
    logInfo(italic("some italic message"))
    logInfo(italic("some ${bold("mixed")} message"))
  }

  @Test
  fun testLogLevels() {
    logInfoBlcks("some info message", LogSource.BLCKS)
    logInfo("some info message without source")

    logErrorBlcks("some error message", LogSource.BLCKS)
    logError("some error message without source")

    logSuccessBlcks("some success message", LogSource.BLCKS)
    logSuccess("some success message without source")

    logWarningBlcks("some warning message", LogSource.BLCKS)
    logWarning("some warning message without source")

    logDebugBlcks("some debug message", LogSource.BLCKS)
    logDebug("some debug message without source")
  }

  @Test
  fun testLogWithDurations() {
    logInfo("some info message with duration 1", duration = 1.seconds)
    logInfo("some info message with duration 2", duration = 2.seconds)
    logInfo("some info message with duration 3", duration = 3.seconds)
    logInfo("some info message with duration 4", duration = 4.seconds)
    logInfo("some info message with duration 5", duration = 5.seconds)
    logInfo("some info message with duration 6", duration = 6.seconds)
  }

  @Test
  fun testLogContextIndentation() {
    var context = LogContext.default()

    logInfo("indent 0", context = context)

    context = context.indent()
    logInfo("indent 1", context = context)

    context = context.indent()
    logInfo("indent 2", context = context)

    context = context.indent()
    logInfo("indent 3", context = context)

    context = context.indent()
    logInfo("indent 4", context = context)

    context = context.indent()
    logInfo("indent 5", context = context)

    context = context.indent()
    logInfo("indent 6", context = context)

    context = context.unindent()
    logInfo("unindent 6", context = context)

    context = context.unindent()
    logInfo("unindent 7", context = context)

    context = context.unindent()
    logInfo("unindent 8", context = context)

    context = context.indent()
    logInfo("indent 9", context = context)

    context = context.indent()
    logInfo("indent 10", context = context)
  }

  @Test
  fun testLogContextTiming() {
    val context = LogContext.withTiming()
    logInfo("some info message with start 1", context = context)
    logDebug("some debug message with start 1", context = context)
    logError("some error message with start 1", context = context)
    logWarning("some warning message with start 1", context = context)
    sleep(250)
    logInfo("some info message with start 2", context = context)
    sleep(250)
    logInfo("some info message with start 3", context = context)
    sleep(250)
    logInfo("some info message with start 4", context = context)
    sleep(250)
    logInfo("some info message with start 5", context = context)
    sleep(250)
    logInfo("some info message with start 6", context = context)
  }
}
