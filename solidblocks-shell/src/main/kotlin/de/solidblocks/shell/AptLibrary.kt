package de.solidblocks.shell

object AptLibrary : ShellLibrary {

  override fun name() = "apt"

  class UpdateRepositories : LibraryCommand {
    override fun commands() = listOf("apt_update_repositories")
  }
}
