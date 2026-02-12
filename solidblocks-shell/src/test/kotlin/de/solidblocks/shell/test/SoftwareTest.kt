package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import localTestContext
import org.junit.jupiter.api.Test

public class SoftwareTest {
  @Test
  fun testEnsureRestic() {
    val result =
        localTestContext()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_restic")
            .step("software_set_export_path")
            .step("restic version")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*restic 0.15.1.*"
    }
  }

  @Test
  fun testEnsureShellcheck() {
    val result =
        localTestContext()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_shellcheck")
            .step("software_set_export_path")
            .step("shellcheck --version")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*version: 0.8.0.*"
    }
  }

  @Test
  fun testEnsureS3Cmd() {
    val result =
        localTestContext()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_s3cmd")
            .step("software_set_export_path")
            .step("s3cmd --version")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*s3cmd version 2.4.0.*"
    }
  }

  @Test
  fun testEnsureTerraform() {
    val result =
        localTestContext()
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_terraform")
            .step("software_set_export_path")
            .step("terraform version")
            .run()

    assertSoftly(result) {
      it shouldHaveExitCode 0
      it stdoutShouldMatch ".*Terraform v1.14.*"
    }
  }
}
