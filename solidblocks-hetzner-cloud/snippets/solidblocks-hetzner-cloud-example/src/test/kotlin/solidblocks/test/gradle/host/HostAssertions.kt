package solidblocks.test.gradle.host

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.portShouldBeClosed
import de.solidblocks.infra.test.assertions.portShouldBeOpen
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class HostAssertions {
    @Test
    fun commandAssertions(testContext: SolidblocksTestContext) {
        val host = testContext.host("pelle.io")

        host portShouldBeOpen 22
        host portShouldBeClosed 80
    }
}