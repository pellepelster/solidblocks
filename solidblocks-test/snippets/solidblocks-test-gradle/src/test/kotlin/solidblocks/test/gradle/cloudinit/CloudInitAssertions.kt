package solidblocks.test.gradle.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldBeSuccess
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit

@ExtendWith(SolidblocksTest::class)
public class CloudInitAssertions {

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val cloudInit = context.cloudInit("<ssh_host>", "<private_key>")

        // ensure cloud-init finished without errors
        cloudInit.result().shouldBeSuccess()

        // dump the cloud-init output log to stdout
        cloudInit.printOutputLog()
    }
}
