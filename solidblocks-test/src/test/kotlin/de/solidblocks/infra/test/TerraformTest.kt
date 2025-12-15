package de.solidblocks.infra.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TerraformTest {

  @OptIn(ExperimentalPathApi::class)
  @BeforeAll
  fun setup(context: SolidblocksTestContext) {
    Paths.get("").resolve(".cache").toAbsolutePath().deleteRecursively()
    Paths.get("").resolve(".bin").toAbsolutePath().deleteRecursively()
  }

  @Test
  fun testTerraformDirDoesNotExist(context: SolidblocksTestContext) {
    val exception = shouldThrow<RuntimeException> { context.terraform(Path.of("invalid")).apply() }
    exception.message shouldBe ("Terraform dir 'invalid' does not exist")
  }

  @Serializable data class OutputType(val name: String)

  @Test
  fun testInitApplyOutput(context: SolidblocksTestContext) {
    val terraform1 = TerraformTest::class.java.getResource("/terraform1").path
    val terraform = context.terraform(Path.of(terraform1))
    terraform.init()
    terraform.apply()
    val output = terraform.output()

    val raw = output.raw()
    raw shouldHaveSize 2
    raw.keys shouldContain "random1"
    // raw["random1"] shouldHaveLength 12

    output.getString("random1") shouldHaveLength 12
    val list = output.getList("json_list", OutputType::class)
    list shouldHaveSize 1
    list[0].name shouldBe "foo"
  }

  @Test
  fun testMultipleVersions(context: SolidblocksTestContext) {
    val terraform1 = TerraformTest::class.java.getResource("/terraform1").path

    val terraformDefaultVersion = context.terraform(Path.of(terraform1))
    terraformDefaultVersion.version() shouldContain "Terraform v1.14.2"

    val terraform1122 = context.terraform(Path.of(terraform1), version = "1.12.2")
    terraform1122.version() shouldContain "Terraform v1.12.2"
  }

  @Test
  fun testMultipleInstances(context: SolidblocksTestContext) {
    val terraform1 = TerraformTest::class.java.getResource("/terraform1").path

    val instance1 = context.terraform(Path.of(terraform1))
    instance1.version() shouldContain "Terraform v1.14.2"

    val instance2 = context.terraform(Path.of(terraform1))
    instance2.version() shouldContain "Terraform v1.14.2"
  }
}
