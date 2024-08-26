package de.solidblocks.infra.test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class SolidblocksExtensionTest {

    @Test
    fun testFilesCleanup(testContext: SolidblocksTestContext) {
        val tempDir = testContext.createTempDir()
    }
}


