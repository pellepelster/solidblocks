package solidblocks.test.gradle.terraform

import de.solidblocks.infra.test.terraform.terraformTestContext
import io.kotest.matchers.string.shouldHaveLength
import org.junit.jupiter.api.Test

public class TerraformTestModule {

    @Test
    fun testTerraformContext() {
        val terraform1 = TerraformTestModule::javaClass.getResource("/module1").path

        val terraform = terraformTestContext(terraform1)
        terraform.init()
        terraform.apply()
        val output = terraform.output()

        output.getString("string1") shouldHaveLength 12

        terraform.destroy()
    }
}
