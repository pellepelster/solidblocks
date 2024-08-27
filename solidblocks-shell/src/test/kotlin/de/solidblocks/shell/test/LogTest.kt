package de.solidblocks.shell.test

import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.infra.test.output.stderrShouldMatch
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import testLocal

public class LogTest {
  @Test
  fun testLogMessages() {
    val result =
        testLocal()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("log.sh"))
            .step("log_info 'some info'")
            .step("log_success 'some success'")
            .step("log_warning 'some warning'")
            .step("log_error 'some error'")
            .step("log_debug 'some debug'")
            .run()

    assertSoftly(result) {
      it stderrShouldMatch ".*2024-.* UTC \\[  success\\] some success.*"
      it stderrShouldMatch ".*2024-.* UTC \\[  warning\\] some warning.*"
      it stderrShouldMatch ".*2024-.* UTC \\[    debug\\] some debug.*"
      it stderrShouldMatch ".*2024-.* UTC \\[    error\\] some error.*"
      it stderrShouldMatch ".*2024-.* UTC \\[     info\\] some info.*"
    }
  }

  @Test
  fun testLogDie() {
    val result =
        testLocal()
            .script()
            .assertSteps(false)
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("log.sh"))
            .step("log_die 'fatal message'")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 4
      it stderrShouldMatch ".*2024-.* UTC \\[emergency\\] fatal message.*"
    }
  }
}
