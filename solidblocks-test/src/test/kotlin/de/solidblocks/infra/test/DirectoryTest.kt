package de.solidblocks.infra.test

import de.solidblocks.infra.test.files.tempDir
import io.kotest.matchers.paths.shouldBeEmptyDirectory
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotBeEmptyDirectory
import io.kotest.matchers.paths.shouldNotExist
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

public class DirectoryTest {

    @Test
    fun testTempDir() {
        val tempDir = tempDir()

        tempDir.path.shouldExist()
        tempDir.path.shouldBeEmptyDirectory()

        Files.writeString(tempDir.path.resolve("some_file"), "some_text")
        tempDir.path.shouldNotBeEmptyDirectory()

        tempDir.clean()
        tempDir.path.shouldExist()
        tempDir.path.shouldBeEmptyDirectory()

        tempDir.remove()
        tempDir.path.shouldNotExist()
    }

    @Test
    fun testTempDirAutoRemove() {
        var path: Path? = null

        val tempDir = tempDir().use {
            path = it.path
            it.path.shouldExist()
        }

        path!!.shouldNotExist()
    }

    @Test
    fun testNestedDir() {
        val tempDir = tempDir()

        tempDir.path.shouldBeEmptyDirectory()

        val dir1 = tempDir.createDir("dir1")
        tempDir.path.shouldNotBeEmptyDirectory()
        dir1.path.shouldBeEmptyDirectory()
        dir1.path.shouldExist()

        val dir2 = tempDir.createDir("dir2")
        tempDir.path.shouldNotBeEmptyDirectory()
        dir2.path.shouldBeEmptyDirectory()
        dir2.path.shouldExist()

        tempDir.clean()

        dir1.path.shouldNotExist()
        dir2.path.shouldNotExist()
        tempDir.path.shouldExist()
        tempDir.path.shouldBeEmptyDirectory()

        tempDir.remove()
        tempDir.path.shouldNotExist()
    }
}