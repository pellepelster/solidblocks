package de.solidblocks.shell

import java.io.BufferedReader

object LogLibrary {
    fun source() = LogLibrary::class.java.classLoader.getResourceAsStream("log.sh").bufferedReader(Charsets.UTF_8).use(
        BufferedReader::readText
    )
}