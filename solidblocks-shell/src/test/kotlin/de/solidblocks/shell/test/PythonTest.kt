package de.solidblocks.shell.test

import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.files.file
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.infra.test.output.stdoutShouldMatch
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import testLocal

public class PythonTest {

    @Test
    fun testEnsureVenv() {

        val tempDir = tempDir()
        tempDir.file("requirements.txt").content("PyJWT==2.6.0").create()

        val result = testLocal().script()
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
