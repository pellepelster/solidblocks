package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.GarageLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

public class GarageTest {
  @Test
  fun testEnsurePackage() {
    val result =
        dockerTestContext(DockerTestImage.DEBIAN_12)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("curl.sh"))
            .includes(workingDir().resolve("lib").resolve("garage.sh"))
            .step("garage_install") { it.fileExists("/usr/local/bin/garage") shouldBe true }
            .run()
    assertSoftly(result) { it shouldHaveExitCode 0 }
  }

  @Test
  fun testLibrarySource() {
    GarageLibrary.source() shouldContain "garage_install"
  }
}
