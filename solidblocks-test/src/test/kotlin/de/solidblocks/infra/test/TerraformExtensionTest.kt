package de.solidblocks.infra.test

import io.kotest.matchers.maps.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class TerraformExtensionTest {

  @Test
  fun testResourcesAreDestroyedAfterTest(context: SolidblocksTestContext) {
    val terraform1 = TerraformExtensionTest::class.java.getResource("/terraformTestBed1").path

    val terraform = context.terraform(terraform1)
    terraform.init()
    terraform.apply()
    val output = terraform.output()
    val raw = output.raw()
    raw shouldHaveSize 5
  }
}
