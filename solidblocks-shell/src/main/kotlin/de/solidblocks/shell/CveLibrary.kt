package de.solidblocks.shell

object CveLibrary : ShellLibrary {
    override fun name() = "cve"

    class FixFragnesia : ShellCommand {
        override fun commands() = listOf("cve_fragnesia")
    }
}
