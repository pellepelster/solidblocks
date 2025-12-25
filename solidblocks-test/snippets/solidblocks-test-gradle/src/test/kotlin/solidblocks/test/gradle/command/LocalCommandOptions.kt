package solidblocks.test.gradle.command

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

@ExtendWith(SolidblocksTest::class)
class LocalCommandOptions {
    @Test
    fun localCommandContext(testContext: SolidblocksTestContext) {
        val command = testContext.local().command("whoami")

        // timeout for running the command
        command.timeout(10.seconds)

        // inherit environment variables from the shell that spawned the units tests
        command.inheritEnv(true)

        // set working directory for command execution
        command.workingDir(Path.of("/tmp"))

        // set environment variable for command
        command.env("ENV_VAR1" to "foo-bar")

        // command.runResult()
    }
}
