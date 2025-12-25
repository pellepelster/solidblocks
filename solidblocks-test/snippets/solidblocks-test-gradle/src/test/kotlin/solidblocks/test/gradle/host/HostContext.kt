package solidblocks.test.gradle.host

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class HostContext {
    @Test
    fun commandAssertions(testContext: SolidblocksTestContext) {
        val host = testContext.host("pelle.io")

    }
}
