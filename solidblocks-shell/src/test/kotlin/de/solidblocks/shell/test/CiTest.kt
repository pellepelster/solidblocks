package de.solidblocks.shell.test

import de.solidblocks.infra.test.output.stdoutShouldMatch
import de.solidblocks.infra.test.script.script
import de.solidblocks.infra.test.shouldHaveExitCode
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test

public class CiTest {

    @Test
    fun testCiDetectedFalse() {
        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("ci.sh"))
            .step("echo ci_detected=\$(ci_detected)")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*ci_detected=false.*"
        }
    }

    @Test
    fun testCiDetectedCi() {
        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("ci.sh"))
            .env("CI" to "true")
            .step("echo ci_detected=\$(ci_detected)")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*ci_detected=true.*"
        }
    }

    @Test
    fun testCiDetectedBuildId() {
        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("ci.sh"))
            .env("BUILD_ID" to "123")
            .step("echo ci_detected=\$(ci_detected)")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*ci_detected=true.*"
        }
    }

}
