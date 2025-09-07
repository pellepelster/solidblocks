package de.solidblocks.cli

import de.solidblocks.cli.docs.ansible.AnsibleCollectionHugoGenerator
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import org.junit.jupiter.api.Test

class AnsibleCollectionHugoGeneratorTest {

  @Test
  fun invalidPath() {
    AnsibleCollectionHugoGenerator(Path("invalid_path"), Path("invalid_path")).run() shouldBe false
  }

  @OptIn(ExperimentalPathApi::class)
  @Test
  fun generateDocumentation() {
    val testbedDir =
        Path(".").toRealPath().resolve("test").resolve("ansible").resolve("test_collection1")
    val targetDir = Path(".").toRealPath().resolve("build").resolve("hugo").resolve("collection1")

    targetDir.deleteRecursively()

    AnsibleCollectionHugoGenerator(testbedDir, targetDir).run() shouldBe true
    targetDir.resolve("_index.md").shouldExist()
    targetDir.resolve("role1.md").shouldExist()
  }
}
