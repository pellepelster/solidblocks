package de.solidblocks.cloud.provisioner.garagefs

import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.ssh.SSHClient
import io.github.oshai.kotlinlogging.KotlinLogging

open class BaseGarageFsProvisioner {

    private val logger = KotlinLogging.logger {}

    suspend fun <T> SSHProvisionerContext.withApiClients(server: HetznerServerLookup, adminToken: GenericSecretLookup, block: suspend (Result<GarageFsApi>) -> T): T {
        val adminToken = this.lookup(adminToken)
        return when (val result = this.createOrGetSshClient(server.name)) {
            is Error<SSHClient> -> block.invoke(Error<GarageFsApi>(result.error))
            is Success -> {
                result.data.portForward(3903) {
                    if (it == null || adminToken == null) {
                        block.invoke(Error("could not establish GarageFS connection for${server.logText()}"))
                    } else {
                        block.invoke(
                            Success(
                                GarageFsApi(adminToken.secret, "http://localhost:$it"),
                            ),
                        )
                    }
                }
            }
        }
    }
}
