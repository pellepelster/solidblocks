package de.solidblocks.shell

interface LibraryCommand {
    fun commands(): List<String>

    fun toShell() = commands().joinToString("\n")
}
