package de.solidblocks.shell

import java.io.BufferedReader

object GarageLibrary {
    fun source() =
        GarageLibrary::class.java.classLoader.getResourceAsStream("garage.sh").bufferedReader(Charsets.UTF_8).use(
            BufferedReader::readText
        )

    class Install :
        LibraryCommand {
        override fun toShell() = listOf("garage_install")
    }
}