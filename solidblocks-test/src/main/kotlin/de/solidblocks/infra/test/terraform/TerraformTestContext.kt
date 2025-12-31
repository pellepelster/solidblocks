package de.solidblocks.infra.test.terraform

import de.solidblocks.infra.test.TestContext
import java.nio.file.Path
import kotlin.io.path.exists

fun terraformTestContext(dir: Path, version: String? = null) = TerraformTestContext(dir, version)

fun terraformTestContext(dir: String, version: String? = null) =
    TerraformTestContext(Path.of(dir), version)

class TerraformTestContext(
    val dir: Path,
    version: String? = null,
    val environment: Map<String, String> = emptyMap(),
) : TestContext() {

  val terraform = Terraform(dir, version, environment)

  init {
    if (!dir.exists()) {
      throw RuntimeException("Terraform dir '$dir' does not exist")
    }
    terraform.ensureTerraformBinary()
  }

  fun apply() = terraform.apply()

  fun destroy() = terraform.destroy()

  fun version() = terraform.version()

  fun init() = terraform.init()

  fun output() = terraform.output()

  fun deleteLocalState() = terraform.deleteLocalState()

  fun addVariable(name: String, value: Any) = terraform.addVariable(name, value)

  fun addEnvironmentVariable(name: String, value: Any) =
      terraform.addEnvironmentVariable(name, value)

  override fun afterAll() {
    destroy()
    super.afterAll()
  }
}
