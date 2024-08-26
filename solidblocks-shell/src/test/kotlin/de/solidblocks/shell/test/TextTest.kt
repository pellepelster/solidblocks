package de.solidblocks.shell.test

import de.solidblocks.infra.test.script
import de.solidblocks.infra.test.shouldHaveExitCode
import de.solidblocks.infra.test.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

public class TextTest {

    private fun getCommandPath(path: String) = Path(this.javaClass.classLoader.getResource(path)!!.path)

    @Test
    fun testTextFormats() {
        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("text.sh"))
            .step("echo \"\${FORMAT_DIM}Dim\${FORMAT_RESET}\"")
            .step("echo \"\${FORMAT_UNDERLINE}Underline\${FORMAT_RESET}\"")
            .step("echo \"\${FORMAT_BOLD}Bold\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_RED}Red\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_GREEN}green\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_YELLOW}yellow\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_BLACK}black\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_BLUE}blue\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_MAGENTA}magenta\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_CYAN}cyan\${FORMAT_RESET}\"")
            .step("echo \"\${COLOR_WHITE}white\${FORMAT_RESET}\"")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
        }
    }

}
