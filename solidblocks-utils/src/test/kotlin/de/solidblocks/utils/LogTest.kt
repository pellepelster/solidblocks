package de.solidblocks.utils

import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow

class LogTest {

    @Test
    fun testMessageTypes() {
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

        logInfo("some info message with duration 1", duration = 1.seconds)
        logInfo("some info message with duration 2", duration = 2.seconds)
        logInfo("some info message with duration 3", duration = 3.seconds)
        logInfo("some info message with duration 4", duration = 4.seconds)
        logInfo("some info message with duration 5", duration = 5.seconds)
        logInfo("some info message with duration 6", duration = 6.seconds)

        val start = markNow()
        logInfo("some info message with start 1", start = start)
        sleep(250)
        logInfo("some info message with start 2", start = start)
        sleep(250)
        logInfo("some info message with start 3", start = start)
        sleep(250)
        logInfo("some info message with start 4", start = start)
        sleep(250)
        logInfo("some info message with start 5", start = start)
        sleep(250)
        logInfo("some info message with start 6", start = start)

    }
}