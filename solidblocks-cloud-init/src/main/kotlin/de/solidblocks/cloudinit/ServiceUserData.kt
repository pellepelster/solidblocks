package de.solidblocks.cloudinit

import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.FilePermissions
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.WriteFile

interface ServiceUserData {
    fun shellScript(): ShellScript
    fun ephemeralScript(): ShellScript = shellScript()
}

enum class Distributor {
    debian,
    ubuntu,
}

fun ShellScript.commonSetup() {
    addLibrary(AptLibrary)
    addLibrary(CurlLibrary)

    addCommand(
        WriteFile(
            "\$nrconf{kernelhints} = -1;".toByteArray(),
            "/etc/needrestart/conf.d/kernel.conf",
            FilePermissions.RW_R__R__,
        ),
    )

    addCommand(AptLibrary.UpdateRepositories())
    addCommand(AptLibrary.UpdateSystem())
    addCommand(AptLibrary.InstallPackage("needrestart"))
}
