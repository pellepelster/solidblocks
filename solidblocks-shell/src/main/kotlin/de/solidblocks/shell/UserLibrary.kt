package de.solidblocks.shell

object UserLibrary {
    class Install : ShellCommand {
        override fun commands() = listOf("garage_install")
    }
}
