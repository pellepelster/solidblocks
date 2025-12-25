package solidblocks.test.gradle.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class CloudInitContext {

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val cloudInit = context.cloudInit("<ssh_host>", "<private_key>")

        // print the output of '/var/log/cloud-init-output.log' in case a
        // test fails. This is disabled by default to avoid accidental leakage
        // of secrets that may be processed during cloud-init runs
        cloudInit.printOutputLogOnTestFailure()
    }
}
