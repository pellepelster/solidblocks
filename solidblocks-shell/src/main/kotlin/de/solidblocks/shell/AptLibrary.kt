package de.solidblocks.shell

object AptLibrary : ShellLibrary {
    override fun name() = "apt"

    class UpdateRepositories : ShellCommand {
        override fun commands() = listOf("apt_update_repositories")
    }

    class UpdateSystem : ShellCommand {
        override fun commands() = listOf("apt_update_system")
    }

    class InstallPackage(val pkg: String) : ShellCommand {
        override fun commands() = listOf("apt_ensure_package $pkg")
    }
}
