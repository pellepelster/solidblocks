package de.solidblocks.shell

object GarageLibrary : ShellLibrary {

  override fun name() = "garage"

  class Install : LibraryCommand {
    override fun commands() = listOf("garage_install")
  }
}
