package de.solidblocks.infra.test.snippets

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.files.file
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class ScriptSnippets {

  @Test
  fun scriptSnippet(testContext: SolidblocksTestContext) {
    val tempDir = testContext.createTempDir()

    val library1 =
        """
        #!/usr/bin/env bash
        some_function() {
            echo "hello world"
        }
        """
            .trimIndent()
    val library1File = tempDir.file("library1.sh").content(library1).executable().create()

    val library2 =
        """
        #!/usr/bin/env bash
        another_function() {
            echo "hello universe"
        }
        """
            .trimIndent()
    val library2File = tempDir.file("library2.sh").content(library2).executable().create()

    val result =
        testContext
            .local()
            .script()
            .includes(library1File)
            .includes(library2File)
            .step("some_function arg1") { it.waitForOutput(".*hello world.*") }
            .step("another_function arg2") { it.waitForOutput(".*hello universe.*") }
            .run()

    result shouldHaveExitCode 0
  }
}
