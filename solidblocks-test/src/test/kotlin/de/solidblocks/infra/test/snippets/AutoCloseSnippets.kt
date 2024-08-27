package de.solidblocks.infra.test.snippets

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.files.file
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class AutoCloseSnippets {

    @Test
    fun autoCloseSnippet(testContext: SolidblocksTestContext) {
        // `tempDir` created from `SolidblocksTestContext` will be
        // auto-deleted when `snippet`is finished
        val tempDir = testContext.createTempDir()

        tempDir.file("some-file.txt").content("some-content").create()
    }
}