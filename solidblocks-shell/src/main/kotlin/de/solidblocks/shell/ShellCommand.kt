package de.solidblocks.shell

interface ShellCommand {
    fun commands(): List<String>

    fun toShell() = commands().joinToString("\n")
}
