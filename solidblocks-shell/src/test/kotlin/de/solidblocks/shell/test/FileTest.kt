package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.infra.test.files.zipFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.util.UUID
import org.junit.jupiter.api.Test
import localTestContext

public class FileTest {
  @Test
  fun testExtractToDirectory() {
    val tempDir = tempDir()
    tempDir.zipFile("file.zip").entry("file1", "content1").entry("file2", "content2").create()

    val extractTempDir = "/tmp/${UUID.randomUUID()}"

    val result =
        localTestContext()
            .script()
            .sources(tempDir)
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("file.sh"))
            .step("file_extract_to_directory file.zip $extractTempDir") {
              it.fileExists("$extractTempDir/file1") shouldBe true
              it.fileExists("$extractTempDir/file2") shouldBe true
              it.fileExists("$extractTempDir/.file.zip.extracted") shouldBe true
            }
            .run()

    assertSoftly(result) { it shouldHaveExitCode 0 }
  }
}
