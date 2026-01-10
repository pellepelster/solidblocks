package de.solidblocks.infra.test.files

import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logInfo
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileBuilder(
    private val path: Path,
    private val name: String,
) {

  private var content = byteArrayOf()

  private var executable = false

  fun content(content: String) = content(content.toByteArray())

  fun content(content: ByteArray) = apply { this.content = content }

  fun toFile() = this.path.resolve(name)

  fun create(): Path {
    val file = this.path.resolve(name)
    File(file.toFile().absolutePath).writeBytes(content)

    logInfo(
        "created file '${file.toFile().absolutePath}' with size ${content.size}",
    )

    val permissions = Files.getPosixFilePermissions(file)
    permissions.add(PosixFilePermission.OWNER_EXECUTE)
    Files.setPosixFilePermissions(file, permissions)

    return file
  }

  fun executable() = apply { this.executable = true }
}

data class ZipFile(val file: Path)

class ZipFileBuilder(
    private val path: Path,
    private val name: String,
    private val context: LogContext = LogContext.withTiming(),
) {

  private val entries = mutableMapOf<String, ByteArray>()

  fun entry(file: String, content: String) = entry(file, content.toByteArray())

  fun entry(file: String, content: ByteArray) = apply { entries[file] = content }

  fun create(): ZipFile {
    val zipFile = this.path.resolve(name)
    ZipOutputStream(FileOutputStream(zipFile.toFile())).use { zipOut ->
      entries.forEach { file ->
        val zipEntry = ZipEntry(file.key)
        zipOut.putNextEntry(zipEntry)
        zipOut.write(file.value)
        zipOut.closeEntry()
      }

      zipOut.close()
    }

    logInfo(
        "created '${zipFile.toFile().absolutePath}' with ${entries.size} entries (${
                entries.map { it.key }.joinToString(", ")
            })",
        context = context,
    )

    return ZipFile(zipFile)
  }
}

fun DirectoryBuilder.zipFile(name: String) = ZipFileBuilder(this.path, name)

fun DirectoryBuilder.file(name: String) = FileBuilder(this.path, name)
