package de.solidblocks.shell

object NetworkLibrary : ShellLibrary {
    override fun name() = "network"

    class AddIpV4(val ip: String) : ShellCommand {
        override fun commands() = listOf("network_add_ipv4 '$ip'")
    }
}
