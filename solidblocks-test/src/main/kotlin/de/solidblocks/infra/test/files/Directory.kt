package de.solidblocks.infra.test.files

import de.solidblocks.utils.logInfo
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class DirectoryBuilder(val path: Path) : Closeable {

  init {
    logInfo("created directory '$path'")
  }

  fun files() = Files.walk(path).filter { it.isRegularFile() }.toList()

  fun files(regex: String): List<Path> {
    val r = regex.toRegex()
    return files().filter { r.matches(it.absolutePathString()) }
  }

  fun directories() = Files.walk(path).filter { it.isDirectory() }.toList()

  fun copyFromDir(dir: Path) {
    if (!dir.exists() || !dir.isDirectory()) {
      throw RuntimeException("path '$path' does not exist or is not a directory")
    }

    dir.copyToRecursively(
        path,
        followLinks = false,
    )
  }

  /** Deletes the content of the directory including all regular files and subdirectories. */
  fun clean() {
    logInfo("deleting content of directory '$path'")
    Files.walk(path)
        .filter {
          it.toAbsolutePath() != path.toAbsolutePath() && (it.isRegularFile() || it.isDirectory())
        }
        .forEach {
          logInfo("deleting  '$it'")
          it.deleteRecursively()
        }
  }

  fun fileFromResource(resource: String): FileBuilder {
    val r =
        this.javaClass.classLoader.getResource(resource)
            ?: throw RuntimeException("resource file '$resource' not found")

    return file(Path(r.path).fileName.name).content(r.readBytes())
  }

  fun fileFromPath(path: Path): FileBuilder {
    if (!path.exists()) {
      throw RuntimeException("path '$path' does not exist")
    }

    return file(path.fileName.name).content(path.readBytes())
  }

  fun createDir(directory: String) =
      DirectoryBuilder(path.resolve(directory)).apply {
        if (!path.exists()) {
          path.createDirectories()
        }
      }

  override fun close() {
    logInfo("deleting directory '$path'")
    path.deleteRecursively()
  }
}

fun tempDir(prefix: String = "test") = DirectoryBuilder(createTempDirectory(prefix))

fun workingDir() = Paths.get("").toAbsolutePath()
