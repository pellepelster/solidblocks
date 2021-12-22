package de.solidblocks.provisioner.minio

import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

class FileFromClasspath {

    companion object {
        private val logger = KotlinLogging.logger {}

        fun ensureFile(classpathSource: String, targetFile: File) {

            if (!targetFile.exists()) {
                logger.info { "file '$targetFile' not found" }

                val sourceFile = FileFromClasspath::class.java.getResourceAsStream(classpathSource)
                    ?: throw RuntimeException("could not load '$classpathSource' from classpath")

                logger.info { "copying '$classpathSource' from classpath to '$targetFile'" }

                Files.copy(sourceFile, targetFile.toPath())
                Files.setPosixFilePermissions(targetFile.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"))
            }
        }
    }
}
