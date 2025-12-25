package solidblocks.test.gradle.terraform

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class TerraformContext {

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val modulePath = TerraformContext::class.java.getResource("/module").path

        val terraform = context.terraform(modulePath)
        terraform.init()
        terraform.apply()

        // destroy will be called automatically when all tests from the class are finished
    }
}
