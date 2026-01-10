package de.solidblocks.infra.test.snippets

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.outputShouldMatch
import de.solidblocks.infra.test.assertions.runtimeShouldBeLessThan
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stderrShouldBeEmpty
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.files.file
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class CommandSnippets {

  @Test
  fun longRunningCommandSnippet(testContext: SolidblocksTestContext) {
    val longRunningCommand =
        """
        #!/usr/bin/env bash
        set -eu -o pipefail

        sleep 2
        echo "something has happened"

        sleep 2
        echo "something else has happened"

        sleep 2
        echo "everything worked"
        """
            .trimIndent()

    val tempDir = testContext.createTempDir()
    val command =
        tempDir.file("long-running-script.sh").content(longRunningCommand).executable().create()

    val result =
        testContext
            .local()
            .command(command)
            .assert { it.waitForOutput(".*something has happened.*") }
            .assert { it.waitForOutput(".*something else has happened.*") }
            .runResult()

    result shouldHaveExitCode 0
    result stdoutShouldMatch ".*everything worked.*"
    result.stderrShouldBeEmpty()
    result runtimeShouldBeLessThan 8.seconds
  }

  @Test
  fun respondToCommandSnippet(testContext: SolidblocksTestContext) {
    val respondToCommand =
        """
        #!/usr/bin/env bash
        set -eu -o pipefail

        echo "please enter name"
        read

        echo "name was entered"
        """
            .trimIndent()

    val tempDir = testContext.createTempDir()
    val command =
        tempDir.file("respond-to-command.sh").content(respondToCommand).executable().create()

    val result =
        testContext
            .local()
            .command(command)
            .assert { it.waitForOutput(".*please enter name.*") { "Steve McQueen" } }
            .assert { it.waitForOutput(".*name was entered.*") }
            .runResult()

    result shouldHaveExitCode 0
  }

  @Test
  fun linearAssertionsSnippet(testContext: SolidblocksTestContext) {
    val longRunningCommand =
        """
        #!/usr/bin/env bash
        set -eu -o pipefail

        sleep 2
        echo "something has happened"

        sleep 2
        echo "something else has happened"

        sleep 2
        echo "everything worked"
        """
            .trimIndent()

    val tempDir = testContext.createTempDir()
    val command =
        tempDir.file("long-running-script.sh").content(longRunningCommand).executable().create()

    runBlocking {
      val run = testContext.local().command(command).run()

      run.waitForOutput(".*something has happened.*")
      run.waitForOutput(".*something else has happened.*")

      val result = run.result()

      result shouldHaveExitCode 0
      result stdoutShouldMatch ".*everything worked.*"
      result.stderrShouldBeEmpty()
      result runtimeShouldBeLessThan 8.seconds
    }
  }

  @Test
  fun localCommandSnippet(testContext: SolidblocksTestContext) {
    val currentUserName = System.getProperty("user.name")
    val result = testContext.local().command("whoami").runResult()

    result shouldHaveExitCode 0
    result outputShouldMatch (".*$currentUserName.*")
    result.stderrShouldBeEmpty()
  }

  @Test
  fun dockerCommandSnippet(testContext: SolidblocksTestContext) {
    val result = testContext.docker(DockerTestImage.UBUNTU_22).command("whoami").runResult()

    result shouldHaveExitCode 0
    result outputShouldMatch (".*root.*")
    result.stderrShouldBeEmpty()
  }

  @Test
  fun dockerCommandsSnippet(testContext: SolidblocksTestContext) {
    listOf(
            DockerTestImage.UBUNTU_20,
            DockerTestImage.UBUNTU_22,
            DockerTestImage.UBUNTU_24,
            DockerTestImage.DEBIAN_11,
            DockerTestImage.DEBIAN_11,
            DockerTestImage.DEBIAN_12,
        )
        .forEach {
          val result = testContext.docker(it).command("whoami").runResult()

          result shouldHaveExitCode 0
          result outputShouldMatch (".*root.*")
          result.stderrShouldBeEmpty()
        }
  }
}
