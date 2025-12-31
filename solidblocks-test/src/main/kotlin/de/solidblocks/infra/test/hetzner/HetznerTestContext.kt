package de.solidblocks.infra.test.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.resources.SSHKeysCreateRequest
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.infra.test.cloudinit.cloudInitTestContext
import de.solidblocks.infra.test.host.hostTestContext
import de.solidblocks.infra.test.log
import de.solidblocks.infra.test.ssh.sshTestContext
import de.solidblocks.ssh.SSHKeyUtils
import kotlinx.coroutines.runBlocking
import java.security.KeyPair

fun hetznerTestContext(hcloudToken: String, testId: String) =
    HetznerTestContext(hcloudToken, testId)

class HetznerServerTestContext(val host: String, val privateKey: String) {

    fun cloudInit(username: String = "root", port: Int = 22) =
        cloudInitTestContext(host, privateKey, username, port)

    fun ssh(username: String = "root", port: Int = 22) =
        sshTestContext(host, privateKey, username, port)

    fun host() =
        hostTestContext(host)
}

class HetznerTestContext(hcloudToken: String, val testId: String) {

    val api = HetznerApi(hcloudToken)

    val testSSHhKey = SSHKeyUtils.RSA.generate()

    fun createServer(userData: String): HetznerServerTestContext = runBlocking {
        removeKey(testId)
        removeServer(testId)

        log("creating ssh-key '$testId'")
        val newSSHKey =
            api.sshKeys.create(
                SSHKeysCreateRequest(
                    testId,
                    SSHKeyUtils.RSA.publicKeyToOpenSsh(testSSHhKey.publicKey),
                ),
            ) ?: throw RuntimeException("failed to create ssh-key '$testId'")

        val newServer =
            api.servers.create(
                ServerCreateRequest(
                    testId,
                    "nbg1",
                    "cx23",
                    "debian-13",
                    sshKeys = listOf(newSSHKey.sshKey.id),
                    userData = userData
                ),
            )

        if (newServer == null || newServer.action == null) {
            throw RuntimeException("error while creating server '$testId'")
        }

        val result =
            api.waitForAction(
                newServer.action.id,
                { log("waiting for server '$testId'") },
                { api.servers.action(it) },
            )

        HetznerServerTestContext(newServer.server.publicNetwork!!.ipv4!!.ip, testSSHhKey.privateKey)
    }

    private suspend fun removeKey(name: String) {
        val sshKey = api.sshKeys.get(name)
        if (sshKey != null) {
            log("removing ssh-key '$name'")
            api.sshKeys.delete(sshKey.id)
        }
    }

    private suspend fun removeServer(name: String) {
        val server = api.servers.get(name)
        if (server != null) {
            log("removing server '$name'")
            api.servers.delete(server.id)
        }
    }
}
