package de.solidblocks.shell

object GarageLibrary : ShellLibrary {
    override fun name() = "garage"

    class Install : ShellCommand {
        override fun commands() = listOf("garage_install")
    }
}
