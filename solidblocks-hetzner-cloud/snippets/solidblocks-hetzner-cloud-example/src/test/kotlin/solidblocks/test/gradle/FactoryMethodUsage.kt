package solidblocks.test.gradle

import de.solidblocks.infra.test.terraform.terraformTestContext
import org.junit.jupiter.api.Test

class FactoryMethodUsage {
    @Test
    fun factoryMethodUsage() {
        val terraformModule = terraformTestContext("some/terraform/module1")
        terraformModule.apply()
    }
}
