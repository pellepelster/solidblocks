package de.solidblocks.infra.test.files

import de.solidblocks.infra.test.log
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.time.TimeSource

@OptIn(ExperimentalPathApi::class)
class DirectoryBuilder(
    val path: Path,
    private val start: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
) : Closeable {

    init {
        log(start, "created directory '$path'")
    }

    fun files() = Files.walk(path).filter { it.isRegularFile() }.toList()

    fun files(regex: String): List<Path> {
        val r = regex.toRegex()
        return files().filter {
            r.matches(it.absolutePathString())
        }
    }

    fun directories() = Files.walk(path).filter { it.isDirectory() }.toList()

    fun copyFromDir(dir: Path) {
        if (!dir.exists() || !dir.isDirectory()) {
            throw RuntimeException("path '$path' does not exist or is not a directory")
        }

        dir.copyToRecursively(
            path, followLinks = false
        )
    }

    /**
     * Deletes the content of the directory  including all regular files and subdirectories.
     */
    fun clean() {
        log(start, "deleting content of directory '$path'")
        Files.walk(path)
            .filter { it.toAbsolutePath() != path.toAbsolutePath() && (it.isRegularFile() || it.isDirectory()) }
            .forEach {
                log(start, "deleting  '$it'")
                it.deleteRecursively()
            }
    }

    fun fileFromResource(resource: String): FileBuilder {
        val r = this.javaClass.classLoader.getResource(resource)
            ?: throw RuntimeException("resource file '$resource' not found")

        return file(Path(r.path).fileName.name).content(r.readBytes())
    }

    fun fileFromPath(path: Path): FileBuilder {
        if (!path.exists()) {
            throw RuntimeException("path '$path' does not exist")
        }

        return file(path.fileName.name).content(path.readBytes())
    }

    fun createDir(directory: String) = DirectoryBuilder(path.resolve(directory)).apply {
        if (!path.exists()) {
            path.createDirectories()
        }
    }

    override fun close() {
        log(start, "deleting directory '$path'")
        path.deleteRecursively()
    }

}

fun tempDir() = DirectoryBuilder(createTempDirectory("test"))

fun workingDir() = Paths.get("").toAbsolutePath()
