package de.solidblocks.infra.test.docker

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import de.solidblocks.infra.test.output.OutputType
import de.solidblocks.infra.test.output.TimestampedOutputLine
import de.solidblocks.utils.LogSource
import de.solidblocks.utils.logInfo
import java.io.Closeable
import kotlin.time.TimeSource

abstract class BaseDockerResultCallback(
    private val start: TimeSource.Monotonic.ValueTimeMark,
    private val output: (entry: TimestampedOutputLine) -> Unit,
) : ResultCallback<Frame> {

  override fun close() {}

  override fun onStart(closeable: Closeable) {}

  override fun onNext(frame: Frame) {
    val payload = frame.payload.decodeToString()
    logInfo(
        payload,
        when (frame.streamType) {
          StreamType.STDOUT -> LogSource.STDOUT
          StreamType.STDERR -> LogSource.STDERR
          else -> {
            throw RuntimeException("unsupported docker log stream type: ${frame.streamType}")
          }
        },
        TimeSource.Monotonic.markNow() - start,
    )

    payload
        .lines()
        .dropLastWhile { it.isEmpty() }
        .forEach {
          output.invoke(
              TimestampedOutputLine(
                  TimeSource.Monotonic.markNow() - start,
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
