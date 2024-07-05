package de.solidblocks.rds.postgresql.test.extensions

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.testcontainers.containers.output.OutputFrame
import java.time.Duration
import java.util.function.Consumer

class TestContainersLogConsumer(private val parent: Consumer<OutputFrame>?) : Consumer<OutputFrame> {

    private val frames = mutableListOf<OutputFrame>()

    override fun accept(frame: OutputFrame) {
        parent?.accept(frame)
        frames.add(frame)
    }

    fun waitForLogLine(logLine: String) {
        logger.info { "[test] waiting for logline '${logLine}'" }
        await.atMost(Duration.ofMinutes(5)).until {
            hasLogLine(logLine)
        }
    }

    fun hasLogLine(logLine: String): Boolean {
        return frames.any {
            it.utf8String.contains(logLine)
        }
    }

    fun assertHasLogLine(logLine: String) {
        logger.info { "[test] asserting logline '${logLine}'" }
        assertThat(hasLogLine(logLine)).isTrue
    }

    fun assertHasNoLogLine(logLine: String) {
        logger.info { "[test] asserting no logline '${logLine}'" }

        assertThat(hasLogLine(logLine)).isFalse
    }

    fun clear() {
        frames.clear()
    }
}