package de.solidblocks.shell

import de.solidblocks.shell.ResticLibrary.RESTIC_CREDENTIALS_PATH

object GithubLibrary : ShellLibrary {
    override fun name() = "github"

    class InstallRunner : ShellCommand {
        override fun commands() = listOf("github_runner_install")
    }
}
