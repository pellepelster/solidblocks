package de.solidblocks.webs3test.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.files.file
import io.kotest.matchers.file.shouldExist
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReleaseTest {

    @Test
    fun testSnippets(context: SolidblocksTestContext) {

        val tempDir = context.createTempDir()
        val version = System.getenv("VERSION") ?: throw RuntimeException("environment variable VERSION is not set")

        val url =
            URI.create("https://github.com/pellepelster/solidblocks/releases/download/${version}/blcks-shell-bootstrap-solidblocks-${version}.sh")
                .toURL()

        val testSh = tempDir.file("test.sh").content(url.readBytes()).create()


        val script = context.docker(DockerTestImage.DEBIAN_12)
            .script()
            .includes(testSh)
            .step("bootstrap_solidblocks")

        script.run()
        script.tempDir.path.resolve(".solidblocks-shell/text.sh").toFile().shouldExist()
    }
}
