package solidblocks.test.gradle

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class ExtensionUsage {
    @Test
    fun extensionUsage(testContext: SolidblocksTestContext) {
        val terraformModule = testContext.terraform("some/terraform/module1")
        terraformModule.apply()
    }
}
