package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.DockerLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

@ExtendWith(SolidblocksTest::class)
public class ShellScriptTest {
    @Test
    fun testFlow() {
        val script = ShellScript()

        /** inline sources are directly included in the rendered script */
        script.addInlineSource(StorageLibrary)
        script.addInlineSource(DockerLibrary)
        script.addInlineSource(AptLibrary)

        script.addCommand(AptLibrary.UpdateRepositories())
        script.addCommand(AptLibrary.InstallPackage("jq"))
        script.addCommand(DockerLibrary.InstallDebian())

        val tempDir = Files.createTempDirectory("test")
        tempDir.resolve("script.sh").writeText(script.render())

        val result =
            dockerTestContext(DockerTestImage.DEBIAN_12)
                .script()
                .timeout(3.minutes)
                .sources(tempDir)
                .includes(tempDir.resolve("script.sh"))
                .run()

        assertSoftly(result) { it shouldHaveExitCode 0 }
    }
}
