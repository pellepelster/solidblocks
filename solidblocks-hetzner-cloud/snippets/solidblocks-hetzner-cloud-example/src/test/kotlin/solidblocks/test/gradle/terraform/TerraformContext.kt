package solidblocks.test.gradle.terraform

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class TerraformContext {

    @Serializable
    data class OutputType(val name: String)

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val modulePath = TerraformContext::class.java.getResource("/module").path

        val terraform = context.terraform(modulePath)
        terraform.init()
        terraform.apply()

        val output = terraform.output()

        val string1 = output.getString("string1")
        val number1 = output.getNumber("number1")
        val boolean1 = output.getBoolean("boolean1")

        val json1: OutputType = output.getObject("json1", OutputType::class)
        val json2: List<OutputType> = output.getList("jsonList1", OutputType::class)

        // destroy will be called automatically when all tests from the class are finished
    }
}
