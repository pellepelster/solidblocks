package de.solidblocks.shell

object PackageLibrary : ShellLibrary {
    override fun name() = "package"

    class UpdateSystem : LibraryCommand {
        override fun commands() = listOf("package_update_system")
    }

    class UpdateRepositories : LibraryCommand {
        override fun commands() = listOf("package_update_repositories")
    }

    class InstallPackage(val pkg: String) : LibraryCommand {
        override fun commands() = listOf("package_ensure_package $pkg")
    }
}
