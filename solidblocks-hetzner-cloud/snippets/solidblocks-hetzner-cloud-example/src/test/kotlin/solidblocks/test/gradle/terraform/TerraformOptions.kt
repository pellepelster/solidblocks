package solidblocks.test.gradle.terraform

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class TerraformOptions {

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val modulePath = TerraformOptions::class.java.getResource("/module").path

        // use a specific Terraform version
        val terraform = context.terraform(modulePath, "1.14.1")

        // set terraform variable (implicitly sets the
        // environment variable 'TF_VAR_variable1'
        terraform.addVariable("variable1", "foo-bar")

        /// set environment variable for the terraform process, can be
        // used to pass credentials to the terraform providers
        terraform.addEnvironmentVariable("PROVIDER_TOKEN", "foo-bar")

        // do not clean up any resources when the test run fails,
        // e.g. destroy will not be executed after test run is finished
        context.cleanupAfterTestFailure(false)
    }
}
