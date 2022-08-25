package de.solidblocks.rds.postgresql.test

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.testcontainers.containers.output.OutputFrame
import java.time.Duration
import java.util.function.Consumer

class TestContainersLogConsumer(val parent: Consumer<OutputFrame>?) : Consumer<OutputFrame> {

    private val frames = mutableListOf<OutputFrame>()

    override fun accept(frame: OutputFrame) {
        parent?.accept(frame)
        frames.add(frame)
    }

    fun waitForLogLine(logLine: String) {
        await.atMost(Duration.ofMinutes(1)).until {
            hasLogLine(logLine)
        }
    }

    fun hasLogLine(logLine: String): Boolean {
        return frames.any {
            it.utf8String.contains(logLine)
        }
    }

    fun assertHasLogLine(logLine: String) {
        assertThat(hasLogLine(logLine)).isTrue
    }

    fun assertHasNoLogLine(logLine: String) {
        assertThat(hasLogLine(logLine)).isFalse
    }

    fun clear() {
        frames.clear()
    }
}