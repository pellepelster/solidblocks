package de.solidblocks.shell

object SystemDLibrary {
  data class Restart(val unit: String) : LibraryCommand {
    override fun commands() = listOf("systemctl restart $unit")
  }

  data class Enable(val unit: String) : LibraryCommand {
    override fun commands() = listOf("systemctl enable $unit")
  }

  data class Start(val unit: String) : LibraryCommand {
    override fun commands() = listOf("systemctl start $unit")
  }

  class DaemonReload : LibraryCommand {
    override fun commands() = listOf("systemctl daemon-reload")
  }
}
