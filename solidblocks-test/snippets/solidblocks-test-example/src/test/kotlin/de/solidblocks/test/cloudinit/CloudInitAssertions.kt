package de.solidblocks.test.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldBeSuccess
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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
