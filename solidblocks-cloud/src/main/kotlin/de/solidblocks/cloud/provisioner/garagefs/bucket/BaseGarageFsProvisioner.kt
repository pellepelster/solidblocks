package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.SecretLookup
import de.solidblocks.ssh.SSHClient
import fr.deuxfleurs.garagehq.api.AccessKeyApi
import fr.deuxfleurs.garagehq.api.BucketApi
import fr.deuxfleurs.garagehq.api.PermissionApi
import java.util.concurrent.Executors
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient

open class BaseGarageFsProvisioner {

  val dispatcher: Dispatcher =
      Dispatcher(
          Executors.newCachedThreadPool {
            val thread = Thread(it)
            thread.setDaemon(true)
            thread
          },
      )

  val client = OkHttpClient.Builder().dispatcher(dispatcher).build()

  data class ApiClients(
      val bucketApi: BucketApi,
      val accessKeyApi: AccessKeyApi,
      val permissionApi: PermissionApi,
  )

  suspend fun <T> ProvisionerContext.withApiClients(
      server: HetznerServerLookup,
      adminToken: SecretLookup,
      block: suspend (ApiClients?) -> T,
  ): T {
    val adminToken = this.ensureLookup(adminToken)
    return this.withPortForward(server) {
      if (it == null) {
        block.invoke(null)
      } else {
        ApiClient.accessToken = adminToken.secret
        block.invoke(
            ApiClients(
                BucketApi("http://localhost:$it", client = client),
                AccessKeyApi("http://localhost:$it", client = client),
                PermissionApi("http://localhost:$it", client = client),
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
