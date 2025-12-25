package de.solidblocks.webs3docker.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import localTestContext

public class CiTest {
  @Test
  fun testCiDetectedFalse() {
    val result =
        localTestContext()
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
        localTestContext()
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
        localTestContext()
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
