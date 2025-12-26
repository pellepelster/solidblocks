package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.files.shouldContainFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReleaseTest {

    @Test
    fun testSnippets(context: SolidblocksTestContext) {
        val version = System.getenv("VERSION") ?: throw RuntimeException("environment variable VERSION is not set")

        val script = context.docker(DockerTestImage.DEBIAN_12)
            .script()
            .includes(URI.create("https://github.com/pellepelster/solidblocks/releases/download/${version}/blcks-shell-bootstrap-solidblocks-${version}.sh"))
            .step("bootstrap_solidblocks")
        script.run()

        script.workingDir shouldContainFile ".solidblocks-shell/text.sh"
    }
}