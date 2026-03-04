package solidblocks.test.gradle

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class TestContexts {
    @Test
    fun extensionUsage(testContext: SolidblocksTestContext) {
        runBlocking {

            val terraformModule = testContext.terraform("some/terraform/module1")
            terraformModule.apply()

            val localCommandContext = testContext.local().command("some_command")
            localCommandContext.run()

            val sshContext = testContext.ssh("<host>", "<private_key>")
            sshContext.fileExists("/some/file")

            val cloudInitContext = sshContext.cloudInit() // ❶
            cloudInitContext.isFinished()

            testContext.cleanupAfterTestFailure(false) // ❷
        }
    }
}
