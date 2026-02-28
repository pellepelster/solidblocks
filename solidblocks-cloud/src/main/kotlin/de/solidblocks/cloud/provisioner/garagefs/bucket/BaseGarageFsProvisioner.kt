package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.ssh.SSHClient

open class BaseGarageFsProvisioner {

    suspend fun <T> ProvisionerContext.withApiClients(
        server: HetznerServerLookup,
        adminToken: PassSecretLookup,
        block: suspend (Result<GarageFsApi>) -> T,
    ): T {
        val adminToken = this.lookup(adminToken)
        return this.withPortForward(server) {
            if (it == null || adminToken == null) {
                block.invoke(Error("no ssh client or admin token"))
            } else {
                block.invoke(
                    Success(
                        GarageFsApi(adminToken.secret, "http://localhost:$it"),
                    ),
                )
            }
        }
    }

    suspend fun <T> ProvisionerContext.withPortForward(
        server: HetznerServerLookup,
        block: suspend (Int?) -> T,
    ): T {
        val server = this.lookup(server)
        return if (server != null) {
            val sshClient = SSHClient(server.publicIpv4!!, this.sshKeyPair, port = server.sshPort)

            sshClient.portForward(3903) { block.invoke(it) }
        } else {
            block.invoke(null)
        }
    }
}
