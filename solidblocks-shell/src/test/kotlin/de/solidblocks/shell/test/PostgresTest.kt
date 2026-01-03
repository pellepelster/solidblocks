package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test

public class PostgresTest {
    @Test
    fun testAddRepository() {
        val tempDir = tempDir()

        val result =
            dockerTestContext(DockerTestImage.DEBIAN_12)
                .script()
                .sources(tempDir)
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("curl.sh"))
                .includes(workingDir().resolve("lib").resolve("postgres.sh"))
                .step("postgres_add_repository")
                .step("postgres_install")
                .step("postgres_current_major_version")
                .run()
        assertSoftly(result) { it shouldHaveExitCode 0 }
    }
}
