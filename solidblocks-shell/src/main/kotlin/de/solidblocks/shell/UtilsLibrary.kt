package de.solidblocks.shell

import java.io.BufferedReader

object UtilsLibrary {
    fun source() = UtilsLibrary::class.java.classLoader.getResourceAsStream("utils.sh").bufferedReader(Charsets.UTF_8).use(
        BufferedReader::readText
    )
}