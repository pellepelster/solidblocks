package de.solidblocks.infra.test

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TerraformTest {

  @Test
  fun testTerraformDirDoesNotExist(context: SolidblocksTestContext) {
    val exception = shouldThrow<RuntimeException> { context.terraform(Path.of("invalid")).apply() }
    exception.message shouldBe ("Terraform dir 'invalid' does not exist")
  }

  @Serializable data class TestOutputType(val name: String)

  @Test
  fun testInitApplyOutputDestroy(context: SolidblocksTestContext) {
    val terraform1 = TerraformTest::class.java.getResource("/terraformTestBed1").path

    val terraform = context.terraform(Path.of(terraform1))
    terraform.init()
    terraform.apply()
    val output = terraform.output()

    val raw = output.raw()
    raw shouldHaveSize 5
    raw.keys shouldContain "string1"
    raw.keys shouldContain "number1"
    raw.keys shouldContain "boolean1"
    raw.keys shouldContain "json1"
    raw.keys shouldContain "json_list1"

    output.getString("string1") shouldBe "foo-bar"
    output.getNumber("number1") shouldBe 123
    output.getBoolean("boolean1") shouldBe true

    assertSoftly(output.getList("json_list1", TestOutputType::class)) {
      it shouldHaveSize 1
      it[0].name shouldBe "foo"
    }

    assertSoftly(output.getObject("json1", TestOutputType::class)) { it.name shouldBe "foo" }

    terraform.apply()
    terraform.destroy()
  }

  @Test
  fun testMultipleVersions(context: SolidblocksTestContext) {
    val terraform1 = TerraformTest::class.java.getResource("/terraformTestBed1").path

    val terraformDefaultVersion = context.terraform(Path.of(terraform1))
    terraformDefaultVersion.version() shouldContain "Terraform v1.14.2"

    val terraform1122 = context.terraform(Path.of(terraform1), version = "1.12.2")
    terraform1122.version() shouldContain "Terraform v1.12.2"
  }

  @Test
  fun testMultipleInstances(context: SolidblocksTestContext) {
    val terraform1 = TerraformTest::class.java.getResource("/terraformTestBed1").path

    val instance1 = context.terraform(Path.of(terraform1))
    instance1.version() shouldContain "Terraform v1.14.2"

    val instance2 = context.terraform(Path.of(terraform1))
    instance2.version() shouldContain "Terraform v1.14.2"
  }
}
