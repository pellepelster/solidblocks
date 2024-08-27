package de.solidblocks.shell.test

import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.infra.test.output.stdoutShouldMatch
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import testLocal

public class CiTest {
  @Test
  fun testCiDetectedFalse() {
    val result =
        testLocal()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("ci.sh"))
            .inheritEnv(false)
            .step("echo ci_detected=\$(ci_detected)")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*ci_detected=false.*"
    }
  }

  @Test
  fun testCiDetectedCi() {
    val result =
        testLocal()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("ci.sh"))
            .inheritEnv(false)
            .env("CI" to "true")
            .step("echo ci_detected=\$(ci_detected)")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*ci_detected=true.*"
    }
  }

  @Test
  fun testCiDetectedBuildId() {
    val result =
        testLocal()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("ci.sh"))
            .inheritEnv(false)
            .env("BUILD_ID" to "123")
            .step("echo ci_detected=\$(ci_detected)")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*ci_detected=true.*"
    }
  }
}
