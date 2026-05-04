package de.solidblocks.cloud.status

import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.context.withCatchingSSHClient
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.MarkdownBuilder
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.ensureCommand

fun MarkdownBuilder.serverStatusMarkdown(status: Result<ServerStatus>) = when (status) {
    is Error<ServerStatus> -> {
        text("*failed to retrieve server status*: **${status.error}**")
    }

    is Success<ServerStatus> -> {
        h3("Kernel")
        if (status.data.needRestart.currentKernel == status.data.needRestart.expectedKernel) {
            text("Kernel ${status.data.needRestart.currentKernel} is up-to-date")
        } else {
            text("Current kernel is ${status.data.needRestart.currentKernel} and an update to **${status.data.needRestart.expectedKernel}** is available")
        }
    }
}

data class ServerStatus(val needRestart: NeedRestart)

fun SSHClient.serverStatus(): ServerStatus {
    val needRestart = this.ensureCommand("needrestart -b").parseNeedRestart()
    return ServerStatus(needRestart)
}

fun <T> SSHProvisionerContext.withServerStatus(serverName: String, block: (SSHClient, ServerStatus) -> T) = this.withCatchingSSHClient(serverName) {
    val status = it.serverStatus()
    block.invoke(it, status)
}
