package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.DockerLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

public class DockerTest {
  @Test
  fun testDockerInstall() {
    val result =
        dockerTestContext(DockerTestImage.DEBIAN_12)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("docker.sh"))
            .step("docker_install_debian") {
              it.fileExists("/etc/apt/keyrings/docker.asc") shouldBe true
            }
            .run()
    assertSoftly(result) { it shouldHaveExitCode 0 }
  }

  @Test
  fun testLibrarySource() {
    DockerLibrary.source() shouldContain "docker_install_debian"
  }
}
