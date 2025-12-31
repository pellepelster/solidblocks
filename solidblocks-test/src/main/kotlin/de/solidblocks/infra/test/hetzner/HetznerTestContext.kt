package de.solidblocks.infra.test.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.resources.SSHKeyResponseWrapper
import de.solidblocks.hetzner.cloud.resources.SSHKeysCreateRequest
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.infra.test.TestContext
import de.solidblocks.infra.test.cloudinit.cloudInitTestContext
import de.solidblocks.infra.test.host.hostTestContext
import de.solidblocks.infra.test.log
import de.solidblocks.infra.test.ssh.sshTestContext
import de.solidblocks.ssh.SSHKeyUtils
import kotlinx.coroutines.runBlocking

fun hetznerTestContext(hcloudToken: String, testId: String) =
    HetznerTestContext(hcloudToken, testId)

class HetznerServerTestContext(val host: String, val privateKey: String) {

  fun cloudInit(username: String = "root", port: Int = 22) =
      cloudInitTestContext(host, privateKey, username, port)

  fun ssh(username: String = "root", port: Int = 22) =
      sshTestContext(host, privateKey, username, port)

  fun host() = hostTestContext(host)
}

class HetznerTestContext(hcloudToken: String, val testId: String) : TestContext {

  val api = HetznerApi(hcloudToken)

  val testSSHhKey = SSHKeyUtils.RSA.generate()

  val defaultLabels = mapOf("blcks.de/test-id" to testId, "blcks.de/managed-by" to "test")

  init {
    log(
        "created hetzner test context, all created resources will be labeled with ${
                defaultLabels.entries.joinToString(", ") {
                    "${it.key}=${it.value}"
                }
            }",
    )
  }

  fun createServer(
      userData: String,
      location: String = "nbg1",
      type: String = "cx23",
      image: String = "debian-13",
      name: String? = null,
  ): HetznerServerTestContext = runBlocking {
    val resourceName = name ?: testId

    val newSSHKey = createSSHKey(resourceName)

    val request =
        ServerCreateRequest(
            resourceName,
            location,
            type,
            image,
            sshKeys = listOf(newSSHKey.sshKey.id),
            userData = userData,
            labels = defaultLabels,
        )
    log(
        "creating server '$resourceName' (${request.type}) in '${request.location}' using image '${request.image}'",
    )
    val newServer = api.servers.create(request)

    if (newServer == null || newServer.action == null) {
      throw RuntimeException("error while creating server '$resourceName'")
    }

    val result =
        api.waitForAction(
            newServer.action.id,
            { log("waiting for provisioning of server '$resourceName'") },
            { api.servers.action(it) },
        )
    if (!result) {
      throw RuntimeException("error while creating server '$resourceName'")
    }

    log("created server '$resourceName' with ip '${newServer.server.publicNetwork!!.ipv4!!.ip}'")
    HetznerServerTestContext(newServer.server.publicNetwork!!.ipv4!!.ip, testSSHhKey.privateKey)
  }

  private suspend fun createSSHKey(resourceName: String): SSHKeyResponseWrapper {
    log("creating ssh-key '$resourceName'")
    val newSSHKey =
        api.sshKeys.create(
            SSHKeysCreateRequest(
                testId,
                SSHKeyUtils.RSA.publicKeyToOpenSsh(testSSHhKey.publicKey),
                defaultLabels,
            ),
        ) ?: throw RuntimeException("failed to create ssh-key '$testId'")
    return newSSHKey
  }

  override fun beforeAll() {
    cleanup()
  }

  override fun cleanup() {
    runBlocking {
      api.sshKeys
          .list(
              labelSelectors =
                  defaultLabels.entries.associate { it.key to LabelSelectorValue.Equals(it.value) },
          )
          .forEach {
            log("cleaning up ssh key '${it.name}' (${it.id})")
            api.sshKeys.delete(it.id)
          }
      api.servers
          .list(
              labelSelectors =
                  defaultLabels.entries.associate { it.key to LabelSelectorValue.Equals(it.value) },
          )
          .forEach {
            log("cleaning up server '${it.name}' (${it.id})")
            api.servers.delete(it.id)
          }
    }
  }
}
