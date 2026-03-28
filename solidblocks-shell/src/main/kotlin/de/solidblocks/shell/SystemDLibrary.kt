package de.solidblocks.shell

object SystemDLibrary {
  data class SystemdRestartService(val service: String) : LibraryCommand {
    override fun commands() = listOf("systemctl restart $service")
  }

  class SystemdDaemonReload : LibraryCommand {
    override fun commands() = listOf("systemctl daemon-reload")
  }
}
