package solidblocks.test.gradle.command

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class DockerCommandContext {
    @Test
    fun dockerCommandContext(testContext: SolidblocksTestContext) {
        val result = testContext.docker(DockerTestImage.UBUNTU_22).command("whoami").runResult()

        result shouldHaveExitCode 0
    }
}