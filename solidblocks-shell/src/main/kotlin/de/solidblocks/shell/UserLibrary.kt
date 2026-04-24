package de.solidblocks.shell

object UserLibrary {
    class AddUserToGroup(val user: String, val group: String) : ShellCommand {
        override fun commands() = listOf(" usermod -aG $group $user")
    }
}
