package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.infra.test.files.file
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import localTestContext
import org.junit.jupiter.api.Test

public class PythonTest {
  @Test
  fun testEnsureVenv() {
    val tempDir = tempDir()
    tempDir.file("requirements.txt").content("PyJWT==2.6.0").create()

    val result =
        localTestContext()
            .script()
            .sources(tempDir)
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("python.sh"))
            .step("python_ensure_venv")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*Successfully installed PyJWT-2.6.0.*"
    }
  }
}
