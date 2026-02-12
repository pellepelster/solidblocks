package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.CaddyLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

public class CaddyTest {
  @Test
  fun testEnsurePackage() {
    val result =
        dockerTestContext(DockerTestImage.DEBIAN_12)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("curl.sh"))
            .includes(workingDir().resolve("lib").resolve("caddy.sh"))
            .step("caddy_install") { it.fileExists("/usr/bin/caddy") shouldBe true }
            .run()

    assertSoftly(result) { it shouldHaveExitCode 0 }
  }

  @Test
  fun testLibrarySource() {
    CaddyLibrary.source() shouldContain "caddy_install"
  }
}
