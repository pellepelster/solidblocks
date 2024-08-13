package de.solidblocks.infra.test

import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class DirectoryBuilder(val path: Path) : Closeable {

    init {
        logger.info { "created directory '$path'" }
    }

    fun files() = Files.walk(path).filter { it.isRegularFile() }.toList()

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
        logger.info { "deleting content of directory '$path'" }
        Files.walk(path)
            .filter { it.toAbsolutePath() != path.toAbsolutePath() && (it.isRegularFile() || it.isDirectory()) }
            .forEach {
                logger.info { "deleting  '$it'" }
                it.deleteRecursively()
            }
    }

    fun createFromResource(resource: String): FileBuilder {
        val r = this.javaClass.classLoader.getResource(resource)
            ?: throw RuntimeException("resource file '$resource' not found")

        return createFile(Path(r.path).fileName.name).content(r.readBytes())
    }

    fun createFromPath(path: Path): FileBuilder {
        if (!path.exists()) {
            throw RuntimeException("path '$path' does not exist")
        }

        return createFile(path.fileName.name).content(path.readBytes())
    }

    fun remove() {
        logger.info { "deleting directory '$path'" }
        path.deleteRecursively()
    }

    fun createDir(directory: String) = DirectoryBuilder(path.resolve(directory)).apply {
        if (!path.exists()) {
            path.createDirectories()
        }
    }

    override fun close() {
        remove()
    }

}

fun tempDir() = DirectoryBuilder(createTempDirectory("test"))

fun workingDir() = Paths.get("").toAbsolutePath()
