package de.solidblocks.infra.test.docker

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import de.solidblocks.infra.test.output.OutputType
import de.solidblocks.infra.test.output.TimestampedOutputLine
import de.solidblocks.utils.LogContext
import java.io.Closeable
import kotlin.time.TimeSource

abstract class BaseDockerResultCallback(
    private val context: LogContext,
    private val output: (entry: TimestampedOutputLine) -> Unit,
) : ResultCallback<Frame> {

  override fun close() {}

  override fun onStart(closeable: Closeable) {}

  override fun onNext(frame: Frame) {
    val payload = frame.payload.decodeToString()

    payload
        .lines()
        .dropLastWhile { it.isEmpty() }
        .forEach {
          output.invoke(
              TimestampedOutputLine(
                  TimeSource.Monotonic.markNow(),
                  it,
                  when (frame.streamType) {
                    StreamType.STDOUT -> OutputType.STDOUT
                    StreamType.STDERR -> OutputType.STDERR
                    else -> {
                      throw RuntimeException(
                          "unsupported docker log stream type: ${frame.streamType}",
                      )
                    }
                  },
              ),
          )
        }
  }
}
