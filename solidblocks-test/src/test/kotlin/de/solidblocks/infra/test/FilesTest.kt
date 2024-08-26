import de.solidblocks.infra.test.createFile
import de.solidblocks.infra.test.zipFile
import de.solidblocks.infra.test.tempDir
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldHaveNameWithoutExtension
import io.kotest.matchers.paths.shouldNotExist
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

public class FilesTest {

    @Test
    fun testCreateFromResource() {
        val tempDir = tempDir()

        tempDir.files() shouldHaveSize 0
        tempDir.createFromResource("command-failure.sh").create()
        tempDir.files() shouldHaveSize 1
        tempDir.files()[0] shouldHaveNameWithoutExtension "command-failure"
    }

    @Test
    fun testCreateFromPath() {
        val tempDir = tempDir()

        tempDir.files() shouldHaveSize 0
        tempDir.createFromPath(Path(this.javaClass.classLoader.getResource("command-failure.sh")!!.path)).create()
        tempDir.files() shouldHaveSize 1
        tempDir.files()[0] shouldHaveNameWithoutExtension "command-failure"
    }

    @Test
    fun testFiles() {
        val tempDir = tempDir()
        tempDir.path.shouldExist()

        tempDir.files() shouldHaveSize 0

        tempDir.zipFile("test.zip")
            .entry("file1.txt", "some content")
            .entry("file2.txt", "some more content").create()

        tempDir.files() shouldHaveSize 1
        tempDir.files()[0] shouldHaveNameWithoutExtension "test"

        val subDir = tempDir.createDir("some_dir")

        subDir.files() shouldHaveSize 0
        subDir.createFile("file1.txt").create()
        subDir.files() shouldHaveSize 1
        tempDir.files() shouldHaveSize 2

        tempDir.clean()
        tempDir.files() shouldHaveSize 0
        tempDir.path.shouldExist()

        tempDir.remove()
        tempDir.path.shouldNotExist()
    }
}