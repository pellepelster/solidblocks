package de.solidblocks.shell

object SystemDLibrary {
    data class SystemdRestartService(val service: String) :
        LibraryCommand {
        override fun toShell() = listOf("systemctl restart ${service}")
    }

    class SystemdDaemonReload() :
        LibraryCommand {
        override fun toShell() = listOf("systemctl daemon-reload")
    }
}