package solidblocks.test.gradle

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.output.outputShouldMatch
import de.solidblocks.infra.test.output.stderrShouldBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class CommandTest {

    @Test
    fun localCommand(testContext: SolidblocksTestContext) {
        val currentUserName = System.getProperty("user.name")
        val result = testContext.local().command("whoami").runResult()

        result shouldHaveExitCode 0
        result outputShouldMatch (".*$currentUserName.*")
        result.stderrShouldBeEmpty()
    }


    @Test
    fun dockerCommand(testContext: SolidblocksTestContext) {
        val result = testContext.docker(DockerTestImage.UBUNTU_22).command("whoami").runResult()

        result shouldHaveExitCode 0
        result outputShouldMatch (".*root.*")
        result.stderrShouldBeEmpty()
    }

}
