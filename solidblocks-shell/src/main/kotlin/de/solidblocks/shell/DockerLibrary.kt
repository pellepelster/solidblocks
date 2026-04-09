package de.solidblocks.shell

object DockerLibrary : ShellLibrary {
    override fun name() = "docker"

    class InstallDebian : LibraryCommand {
        override fun commands() = listOf("docker_install_debian")
    }
}
