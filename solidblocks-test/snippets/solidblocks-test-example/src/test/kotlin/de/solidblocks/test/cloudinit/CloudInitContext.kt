package de.solidblocks.test.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit

@ExtendWith(SolidblocksTest::class)
public class CloudInitContext {

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val cloudInit = context.cloudInit("<ssh_host>", "<private_key>")

        // print the output of '/var/log/cloud-init-output.log' in case a
        // test fails. This is disabled by default to avoid accidental leakage
        // of secrets that may be processed during cloud-init runs
        cloudInit.printOutputLogOnTestFailure()

        // 'isFinished()' will return true, when
        // the '/var/lib/cloud/instance/boot-finished'
        // file is present
        await().atMost(1, TimeUnit.MINUTES).withPollInterval(ofSeconds(5)).until {
            cloudInit.isFinished()
        }
    }
}
