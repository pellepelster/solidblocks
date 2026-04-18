package de.solidblocks.shell

object SSHLibrary : ShellLibrary {
    override fun name() = "ssh"

    class Restore(val repository: String) : ShellCommand {
        override fun commands() = listOf("restic_restore '$repository'")
    }
}
