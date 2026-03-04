package solidblocks.test.gradle.command

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class LocalCommandContext {
    @Test
    fun localCommandContext(testContext: SolidblocksTestContext) {
        val result = testContext.local().command("whoami").runResult()

        result shouldHaveExitCode 0
    }
}
