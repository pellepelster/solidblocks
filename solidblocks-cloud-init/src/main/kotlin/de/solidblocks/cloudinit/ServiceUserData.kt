package de.solidblocks.cloudinit

import de.solidblocks.shell.ShellScript

interface ServiceUserData {
    fun shellScript(): ShellScript
    fun ephemeralScript(): ShellScript = shellScript()
}
