package de.solidblocks.cli.base

import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.ByteArrayInputStream
import java.io.InputStream

class ClassPathLocalSourceFile(val fileName: String) : InMemorySourceFile() {

    val content: ByteArray

    init {
        content = this::class.java.classLoader.getResource(fileName).readBytes()
    }

    override fun getName(): String {
        return fileName
    }

    override fun getLength(): Long {
        return content.size.toLong()
    }

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(content)
    }
}
