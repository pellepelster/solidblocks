package de.solidblocks.rds.postgresql.test.extensions

import java.time.Duration
import java.util.function.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.testcontainers.containers.output.OutputFrame

class TestContainersLogConsumer(private val parent: Consumer<OutputFrame>?) :
    Consumer<OutputFrame> {

  private val frames = mutableListOf<OutputFrame>()

  override fun accept(frame: OutputFrame) {
    parent?.accept(frame)
    frames.add(frame)
  }

  fun waitForLogLine(regex: Regex) {
    logger.info { "[test] waiting for logline '$regex'" }
    await.atMost(Duration.ofMinutes(5)).until { hasLogLine(regex) }
  }

  fun waitForAnyLogLine(regexes: List<Regex>) {
    logger.info {
      "[test] waiting one of the following loglines ${regexes.joinToString(",") { "'$it'" }}"
    }
    await.atMost(Duration.ofMinutes(5)).until { hasLogLine(regexes) }
  }

  fun waitForLogLine(logLine: String) {
    logger.info { "[test] waiting for logline '$logLine'" }
    await.atMost(Duration.ofMinutes(5)).until { hasLogLine(logLine) }
  }

  fun hasLogLine(logLine: String): Boolean = frames.any { it.utf8String.contains(logLine) }

  fun hasLogLine(regex: Regex): Boolean = frames.any { it.utf8String.contains(regex) }

  fun hasLogLine(regexes: List<Regex>): Boolean =
      frames.any { frame -> regexes.any { frame.utf8String.contains(it) } }

  fun assertHasLogLine(logLine: String) {
    logger.info { "[test] asserting logline '$logLine'" }
    assertThat(hasLogLine(logLine)).isTrue
  }

  fun assertHasLogLine(regex: Regex) {
    logger.info { "[test] asserting logline '$regex'" }
    assertThat(hasLogLine(regex)).isTrue
  }

  fun assertHasNoLogLine(logLine: String) {
    logger.info { "[test] asserting no logline '$logLine'" }
    assertThat(hasLogLine(logLine)).isFalse
  }

  fun clear() {
    frames.clear()
  }
}
