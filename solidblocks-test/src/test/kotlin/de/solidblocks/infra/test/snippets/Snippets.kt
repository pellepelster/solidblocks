package de.solidblocks.infra.test.snippets

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.files.file
import de.solidblocks.infra.test.files.matchSingleFile
import de.solidblocks.infra.test.files.shouldContainNFiles
import de.solidblocks.infra.test.files.shouldHaveChecksum
import de.solidblocks.infra.test.files.shouldHaveContent
import de.solidblocks.infra.test.files.singleFile
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.infra.test.files.zipFile
import io.kotest.matchers.paths.shouldNotExist
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.io.path.writeText

@ExtendWith(SolidblocksTest::class)
public class Snippets {

    private fun reverseFile1Content(path: Path) {
        println("reverseFile1Content was called")
        val file1 = path.resolve("file1.txt")
        val content = file1.readText().split(" ")
        file1.writeText(content.reversed().joinToString(" "))
    }

    private fun deleteTest1Zip(path: Path) {
        println("deleteTest1Zip was called")
        path.resolve("test1.zip").deleteExisting()
    }

    private fun createFileWithUnpredictableName(path: Path) {
        println("createFileWithUnpredictableName was called")
        path.resolve("unpredictable_${UUID.randomUUID()}.txt").writeText("unpredictable file content")
    }

    @Test
    fun filesAndDirectories() {
        val tempDir = tempDir()

        // set up testbed by creating files from various sources
        tempDir.file("file1.txt").content("some file1 content").create()
        tempDir.fileFromResource("snippets/file-from-classpath.txt").create()
        tempDir.fileFromPath(workingDir().resolve("src/test/resources/snippets/file-from-path.txt")).create()
        tempDir.zipFile("test1.zip")
            .entry("file2.txt", "some file2 content")
            .entry("file3.txt", "some file3 content").create()

        // assert testbed is ready to go
        tempDir shouldContainNFiles 4

        // call some code working on the testbed
        reverseFile1Content(tempDir.path)
        deleteTest1Zip(tempDir.path)
        createFileWithUnpredictableName(tempDir.path)

        // assert result of "business" code
        tempDir shouldContainNFiles 4
        tempDir singleFile ("file1.txt") shouldHaveContent "content file1 some"
        tempDir singleFile ("file1.txt") shouldHaveChecksum "46cf7a4ae492a815c35a5a17395fee774f2fb2811ec3015b7c64b98a6238077a"
        tempDir.singleFile("test1.zip").shouldNotExist()
        tempDir.matchSingleFile(".*unpredictable_.*") shouldHaveContent "unpredictable file content"

        // remove all files for another test
        tempDir.clean()

        // call some code working on the testbed
        createFileWithUnpredictableName(tempDir.path)

        // assert result again
        tempDir shouldContainNFiles 1
        tempDir matchSingleFile (".*unpredictable_.*") shouldHaveContent "unpredictable file content"

        // remove temporary directory
        tempDir.close()
    }

    @Test
    fun autoCleanResources(testContext: SolidblocksTestContext) {
        val tempDir = testContext.createTempDir()

        tempDir.file("some-file.txt").content("some-content").create()
    }
}