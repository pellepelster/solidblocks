package de.solidblocks.cloud.provisioner.garagefs

import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.provisioner.withPortForward
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.GarageFsApi
import io.github.oshai.kotlinlogging.KotlinLogging

open class BaseGarageFsProvisioner {

    private val logger = KotlinLogging.logger {}

    suspend fun <T> CloudProvisionerContext.withApiClients(server: HetznerServerLookup, adminToken: PassSecretLookup, block: suspend (Result<GarageFsApi>) -> T): T {
        val adminToken = this.lookup(adminToken)
        return this.withPortForward(server, 3903) {
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
