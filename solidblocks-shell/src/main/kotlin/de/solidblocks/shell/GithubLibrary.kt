package de.solidblocks.shell

object GithubLibrary : ShellLibrary {
    override fun name() = "github"

    class InstallRunner : ShellCommand {
        override fun commands() = listOf("github_runner_install")
    }
}
