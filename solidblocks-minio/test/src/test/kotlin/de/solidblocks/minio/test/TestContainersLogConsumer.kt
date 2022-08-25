package de.solidblocks.minio.test

import org.awaitility.kotlin.await
import org.testcontainers.containers.output.OutputFrame
import java.util.function.Consumer

class TestContainersLogConsumer(val parent: Consumer<OutputFrame>?) : Consumer<OutputFrame> {

    private val frames = mutableListOf<OutputFrame>()

    override fun accept(frame: OutputFrame) {
        parent?.accept(frame)
        frames.add(frame)
    }

    fun waitForLogLine(logLine: String) {
        await.until {
            hasLogLine(logLine)
        }
    }

    fun hasLogLine(logLine: String): Boolean {
        return frames.any {
            it.utf8String.contains(logLine)
        }
    }
}