package de.solidblocks.shell.test

import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.testDocker
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

public class PackageTest {
  @Test
  fun testEnsurePackage() {
    val result =
        testDocker(DockerTestImage.DEBIAN_10)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("package.sh"))
            .step("package_update_repositories")
            .step("package_update_system") { it.fileExists("/usr/bin/wget") shouldBe false }
            .step("package_ensure_package wget") { it.fileExists("/usr/bin/wget") shouldBe true }
            .step("package_ensure_package wget") { it.fileExists("/usr/bin/wget") shouldBe true }
            .run()

    assertSoftly(result) { it shouldHaveExitCode 0 }
  }
}
