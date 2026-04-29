package de.solidblocks.shell

object GithubLibrary : ShellLibrary {
    override fun name() = "github"

    class InstallRunner(val runnerHome: String) : ShellCommand {
        override fun commands() = listOf("github_runner_install $runnerHome")
    }
}
