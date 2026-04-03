package de.solidblocks.shell

object CaddyLibrary : ShellLibrary {

  override fun name() = "caddy"

  class Install : LibraryCommand {
    override fun commands() = listOf("caddy_install")
  }
}
