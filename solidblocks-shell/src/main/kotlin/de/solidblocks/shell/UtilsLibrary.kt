package de.solidblocks.shell

object UtilsLibrary : ShellLibrary {
    override fun name() = "utils"

    data class Comment(val comment: String) : ShellCommand {
        override fun commands() = listOf("# $comment")
    }
}
