package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.DockerLibrary
import de.solidblocks.shell.PackageLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import io.kotest.assertions.assertSoftly
import java.nio.file.Files
import kotlin.io.path.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class ShellScriptTest {

  @Test
  fun testFlow() {
    val script = ShellScript()

    /** inline sources are directly included in the rendered script */
    script.addInlineSource(StorageLibrary)
    script.addInlineSource(DockerLibrary)
    script.addInlineSource(AptLibrary)

    /** library sources are written to ShellScript.LIB_SOURCES_PATH and sources from there */
    script.addLibSources(PackageLibrary)

    script.addCommand(PackageLibrary.UpdateRepositories())
    script.addCommand(PackageLibrary.InstallPackage("jq"))
    script.addCommand(DockerLibrary.InstallDebian())

    val rawScript = script.render()

    val tempDir = Files.createTempDirectory("test")
    tempDir.resolve("script.sh").writeText(script.render())

    val result =
        dockerTestContext(DockerTestImage.DEBIAN_12)
            .script()
            .sources(tempDir)
            .includes(tempDir.resolve("script.sh"))
            .run()

    assertSoftly(result) { it shouldHaveExitCode 0 }
  }
}
