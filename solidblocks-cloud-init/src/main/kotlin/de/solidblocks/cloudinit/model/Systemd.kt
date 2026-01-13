package de.solidblocks.cloudinit.model

import de.solidblocks.shell.LibraryCommand

data class SystemdRestartService(val service: String) :
    LibraryCommand {
    override fun toShell() = listOf("systemctl restart ${service}")
}

class SystemdDaemonReload() :
    LibraryCommand {
    override fun toShell() = listOf("systemctl daemon-reload")
}
