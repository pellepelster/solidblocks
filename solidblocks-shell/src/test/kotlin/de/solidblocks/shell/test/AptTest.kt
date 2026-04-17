package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.AptLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

public class AptTest {
    @Test
    fun testLibrarySource() {
        AptLibrary.source() shouldContain "apt_update_repositories"
    }

    @Test
    fun testEnsurePackage() {
        val result =
            dockerTestContext(DockerTestImage.DEBIAN_12)
                .script()
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("apt.sh"))
                .step("apt_update_repositories")
                .step("apt_update_system") { it.fileExists("/usr/bin/wget") shouldBe false }
                .step("apt_ensure_package wget") { it.fileExists("/usr/bin/wget") shouldBe true }
                .step("apt_ensure_package wget") { it.fileExists("/usr/bin/wget") shouldBe true }
                .run()

        assertSoftly(result) { it shouldHaveExitCode 0 }
    }
}
