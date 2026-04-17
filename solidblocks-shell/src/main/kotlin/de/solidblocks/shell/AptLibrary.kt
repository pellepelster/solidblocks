package de.solidblocks.shell

object AptLibrary : ShellLibrary {
    override fun name() = "apt"

    class UpdateRepositories : LibraryCommand {
        override fun commands() = listOf("apt_update_repositories")
    }

    class UpdateSystem : LibraryCommand {
        override fun commands() = listOf("apt_update_system")
    }

    class InstallPackage(val pkg: String) : LibraryCommand {
        override fun commands() = listOf("apt_ensure_package $pkg")
    }
}
