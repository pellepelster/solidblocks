package de.solidblocks.shell.test

import de.solidblocks.infra.test.files.createFile
import de.solidblocks.infra.test.output.stdoutShouldMatch
import de.solidblocks.infra.test.script.script
import de.solidblocks.infra.test.shouldHaveExitCode
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test

public class PythonTest {

    @Test
    fun testEnsureVenv() {

        val tempDir = tempDir()
        tempDir.createFile("requirements.txt").content("PyJWT==2.6.0").create()

        val result = script()
            .sources(tempDir)
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("python.sh"))
            .step("python_ensure_venv")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*Successfully installed PyJWT-2.6.0.*"
        }
    }
}
