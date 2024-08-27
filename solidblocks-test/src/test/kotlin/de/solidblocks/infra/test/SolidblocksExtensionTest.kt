package de.solidblocks.infra.test

import de.solidblocks.infra.test.files.file
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class SolidblocksExtensionTest {

  @Test
  fun testFilesCleanup(testContext: SolidblocksTestContext) {
    val tempDir = testContext.createTempDir()
    tempDir.file("some_file").create()
  }
}
