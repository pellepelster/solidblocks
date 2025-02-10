package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.infra.test.output.stderrShouldMatch
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class LogTest {

  @RepeatedTest(value = 10, failureThreshold = 1)
  fun testLogMessages(testContext: SolidblocksTestContext) {
    val result =
        testContext
            .local()
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
      it stderrShouldMatch ".*2025-.* UTC \\[  success\\] some success.*"
      it stderrShouldMatch ".*2025-.* UTC \\[  warning\\] some warning.*"
      it stderrShouldMatch ".*2025-.* UTC \\[    debug\\] some debug.*"
      it stderrShouldMatch ".*2025-.* UTC \\[    error\\] some error.*"
      it stderrShouldMatch ".*2025-.* UTC \\[     info\\] some info.*"
    }
  }

  @Test
  fun testLogDie(testContext: SolidblocksTestContext) {
    val result =
        testContext
            .local()
            .script()
            .assertSteps(false)
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("log.sh"))
            .step("log_die 'fatal message'")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 4
      it stderrShouldMatch ".*2025-.* UTC \\[emergency\\] fatal message.*"
    }
  }
}
