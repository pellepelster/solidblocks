package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.GarageLibrary
import de.solidblocks.shell.GithubLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

public class GithubTest {
    @Test
    fun testRunnerInstall() {
        val result =
            dockerTestContext(DockerTestImage.DEBIAN_12, timeout = 3.minutes)
                .script()
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("curl.sh"))
                .includes(workingDir().resolve("lib").resolve("apt.sh"))
                .includes(workingDir().resolve("lib").resolve("github.sh"))
                .step("apt_update_repositories")
                .step("apt_ensure_package curl")
                .step("apt_ensure_package ca-certificates")
                .step("github_runner_install") { it.fileExists("/home/github-runner/run.sh") shouldBe true }
                .run()
        assertSoftly(result) { it shouldHaveExitCode 0 }
    }

    @Test
    fun testLibrarySource() {
        GithubLibrary.source() shouldContain "github_runner_install"
    }
}
