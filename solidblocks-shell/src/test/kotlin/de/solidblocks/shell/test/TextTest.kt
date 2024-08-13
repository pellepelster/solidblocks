import de.solidblocks.infra.test.runLocal
import de.solidblocks.infra.test.script
import de.solidblocks.infra.test.shouldHaveExitCode
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.Path

public class TextTest {

    private fun getCommandPath(path: String) = Path(this.javaClass.classLoader.getResource(path)!!.path)

    @Test
    fun testTextFormats() {

        val source = Paths.get("").toAbsolutePath().resolve("lib")
        val path = Paths.get("").toAbsolutePath().resolve("lib").resolve("text.sh")

        val result = script()
            .sources(source)
            .includes(path)
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
