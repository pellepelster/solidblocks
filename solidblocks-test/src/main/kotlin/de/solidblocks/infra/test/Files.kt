package de.solidblocks.infra.test

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


data class File(val file: Path)

class FileBuilder(private val path: Path, private val name: String) {

    private var content = byteArrayOf()

    private var executable = false

    fun content(content: String) = content(content.toByteArray())

    fun content(content: ByteArray) = apply {
        this.content = content
    }

    fun create(): de.solidblocks.infra.test.File {
        val file = this.path.resolve(name)
        File(file.toFile().absolutePath).writeBytes(content)
        logger.info {
            "created file '${file.toFile().absolutePath}' with size ${content.size}"
        }

        val permissions = Files.getPosixFilePermissions(file)
        permissions.add(PosixFilePermission.OWNER_EXECUTE)
        Files.setPosixFilePermissions(file, permissions)

        return File(file)
    }

    fun executable() = apply {
        this.executable = true
    }
}

data class ZipFile(val file: Path)

class ZipFileBuilder(private val path: Path, private val name: String) {

    private val files = mutableMapOf<String, ByteArray>()

    fun addFile(file: String, content: String) = addFile(file, content.toByteArray())

    fun addFile(file: String, content: ByteArray) = apply {
        files.put(file, content)
    }

    fun create(): ZipFile {
        val zipFile = this.path.resolve(name)
        ZipOutputStream(FileOutputStream(zipFile.toFile())).use { zipOut ->
            files.forEach { file ->
                val zipEntry = ZipEntry(file.key)
                zipOut.putNextEntry(zipEntry)
                zipOut.write(file.value)
                zipOut.closeEntry()
            }

            zipOut.close()
        }

        logger.info {
            "created '${zipFile.toFile().absolutePath}' with ${files.size} entries (${
                files.map { it.key }.joinToString(", ")
            })"
        }

        return ZipFile(zipFile)
    }

}

fun DirectoryBuilder.createFile() = apply { }

fun DirectoryBuilder.createZipFile(name: String) = ZipFileBuilder(this.path, name)

fun DirectoryBuilder.createFile(name: String) = FileBuilder(this.path, name)