package de.solidblocks.infra.test

import de.solidblocks.infra.test.files.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.should
import io.kotest.matchers.string.startWith
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

public class FilesTest {

    @Test
    fun testFileFromResource() {
        val tempDir = tempDir()

        tempDir shouldContainNFiles 0
        tempDir.fileFromResource("command-failure.sh").create()
        tempDir shouldContainNFiles 1

        tempDir.files()[0] shouldHaveName "command-failure.sh"
    }

    @Test
    fun testFile() {
        val tempDir = tempDir()

        tempDir shouldContainNFiles 0

        tempDir.file("somefile.txt").content("somefile content").create()

        tempDir shouldContainNFiles 1
        tempDir.files()[0] shouldHaveName "somefile.txt"
        tempDir.files()[0] shouldHaveContent "somefile content"
    }

    @Test
    fun testFilesMatcher() {
        val tempDir = tempDir()

        tempDir.file("somefile.txt").content("somefile content").create()
        tempDir.file("anotherfile.txt").content("anotherfile content").create()

        tempDir.files(".*somefile.txt.*") shouldHaveSize 1
        tempDir.files(".*xxx.*") shouldHaveSize 0
        tempDir.files(".*file.*") shouldHaveSize 2

        tempDir.matchSingleFile(".*somefile.txt.*") shouldHaveChecksum "78d656f38710e2366b6aac471ba207d38efa3720ae383b8b31f0384db6bbd8c4"
        tempDir.matchSingleFile(".*anotherfile.txt.*") shouldHaveChecksum "d922d8fba18c13a1f24d53aa6e37e37d6c4e15d9d1661a9b9dca6697fd65c0dd"

        tempDir.singleFile("anotherfile.txt") shouldHaveChecksum "d922d8fba18c13a1f24d53aa6e37e37d6c4e15d9d1661a9b9dca6697fd65c0dd"

        val exception = shouldThrow<RuntimeException> {
            tempDir.matchSingleFile(".*file.*")
        }
        exception.message should startWith("expected regex '.*file.*' to match exactly one file, but it matched")

    }

    @Test
    fun testFileFromPath() {
        val tempDir = tempDir()

        tempDir shouldContainNFiles 0
        tempDir.fileFromPath(Path(this.javaClass.classLoader.getResource("command-failure.sh")!!.path)).create()

        tempDir shouldContainNFiles 1
        tempDir.files()[0] shouldHaveName "command-failure.sh"
    }

    @Test
    fun testZipFile() {
        val tempDir = tempDir()

        tempDir shouldContainNFiles 0
        tempDir.zipFile("test.zip")
            .entry("file1.txt", "some content")
            .entry("file2.txt", "some more content").create()

        tempDir shouldContainNFiles 1
        tempDir.files()[0] shouldHaveName "test.zip"
    }

    @Test
    fun testFiles() {
        val tempDir = tempDir()
        tempDir.path.shouldExist()

        tempDir shouldContainNFiles 0

        tempDir.zipFile("test.zip")
            .entry("file1.txt", "some content")
            .entry("file2.txt", "some more content").create()

        tempDir.files() shouldHaveSize 1
        tempDir.files()[0] shouldHaveName "test.zip"

        val subDir = tempDir.createDir("some_dir")

        subDir shouldContainNFiles 0
        subDir.file("file1.txt").create()
        subDir shouldContainNFiles 1
        tempDir shouldContainNFiles 2

        tempDir.clean()
        tempDir shouldContainNFiles 0
        tempDir.path.shouldExist()

        tempDir.close()
        tempDir.path.shouldNotExist()
    }
}