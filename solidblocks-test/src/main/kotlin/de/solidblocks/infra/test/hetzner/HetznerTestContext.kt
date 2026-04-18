package de.solidblocks.infra.test.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.HetznerApiErrorType
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.SSHKeysCreateRequest
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeFormat
import de.solidblocks.infra.test.TestContext
import de.solidblocks.infra.test.cloudinit.cloudInitTestContext
import de.solidblocks.infra.test.host.hostTestContext
import de.solidblocks.infra.test.ssh.sshTestContext
import de.solidblocks.infra.test.testLabels
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.ssh.toPem
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logWarning
import kotlinx.coroutines.runBlocking

fun hetznerTestContext(hcloudToken: String, testId: String) = HetznerTestContext(hcloudToken, testId)

class HetznerServerTestContext(val id: Long, val host: String, val privateKey: String, testId: String? = null) : TestContext(testId) {
    fun cloudInit(username: String = "root", port: Int = 22) = cloudInitTestContext(host, privateKey, username, port, testId).also { testContexts.add(it) }

    fun ssh(username: String = "root", port: Int = 22) = sshTestContext(host, privateKey, username, port, testId).also { testContexts }

    fun host(testId: String? = null) = hostTestContext(host, testId)
}

class HetznerTestContext(hcloudToken: String, testId: String? = null) : TestContext(testId) {
    val defaultLocation = HetznerLocation.nbg1

    val api = HetznerApi(hcloudToken)

    val testSSHhKey = SSHKeyUtils.ED25519.generate().toPem()

    val defaultLabels = testLabels(this.testId)

    init {
        logInfo(
            "created hetzner test context, all created resources will be labeled with ${
                defaultLabels.entries.joinToString(", ") {
                    "${it.key}=${it.value}"
                }
            }",
        )

        logWarning(
            "after test completion, alls resources with the labels ${
                defaultLabels.entries.joinToString(", ") {
                    "${it.key}=${it.value}"
                }
            } will be removed",
        )
    }

    fun createServer(
        userData: String,
        sshKey: Long,
        location: HetznerLocation? = null,
        type: HetznerServerType = HetznerServerType.cx23,
        image: String = "debian-13",
        name: String? = null,
        volumes: List<Long>? = null,
    ): HetznerServerTestContext = runBlocking {
        val resourceName = name ?: testId

        val request =
            ServerCreateRequest(
                resourceName,
                location ?: defaultLocation,
                type,
                image,
                sshKeys = listOf(sshKey),
                userData = userData,
                labels = defaultLabels,
                volumes = volumes,
            )
        logInfo(
            "creating server '$resourceName' (${request.type}) in '${request.location}' using image '${request.image}'",
        )
        val newServer = api.servers.create(request)

        val result =
            api.waitForAction(
                newServer.action.id,
                { logInfo("waiting for provisioning of server '$resourceName'") },
                { api.servers.action(it) },
            )
        if (!result) {
            throw RuntimeException("error while creating server '$resourceName'")
        }

        logInfo(
            "created server '$resourceName' with ip '${newServer.server.publicNetwork!!.ipv4!!.ip}'",
        )
        HetznerServerTestContext(
            newServer.server.id,
            newServer.server.publicNetwork!!.ipv4!!.ip,
            testSSHhKey.privateKey,
        )
            .also { testContexts.add(it) }
    }

    fun createSSHKey(name: String? = null): Long = runBlocking {
        val resourceName = name ?: testId
        logInfo("creating ssh-key '$resourceName'")
        val newSSHKey =
            api.sshKeys.create(
                SSHKeysCreateRequest(
                    testId,
                    SSHKeyUtils.ED25519.publicKeyToOpenSsh(testSSHhKey.publicKey),
                    defaultLabels,
                ),
            )
        newSSHKey.sshKey.id
    }

    override fun cleanUp() {
        cleanup()
    }

    fun cleanup() {
        runBlocking {
            api.dnsZones.list().forEach { zone ->
                logInfo("cleaning up dns zone '${zone.name}'")
                api.dnsRrSets(zone.id.toString())
                    .list(labelSelectors = defaultLabels.toLabelSelectors())
                    .forEach {
                        logInfo("cleaning up dns record '${it.name}/${it.type}'")
                        try {
                            api.dnsRrSets(zone.id.toString()).delete(it.id, it.type)
                        } catch (e: Exception) {
                            logWarning("failed to clean up dns record '${it.name}/${it.type}'")
                        }
                    }
            }

            api.sshKeys
                .list(
                    labelSelectors = defaultLabels.toLabelSelectors(),
                )
                .forEach {
                    logInfo("cleaning up ssh key '${it.name}' (${it.id})")
                    api.sshKeys.delete(it.id)
                }
            api.volumes.list(labelSelectors = defaultLabels.toLabelSelectors()).forEach {
                try {
                    logInfo("cleaning up volume '${it.name}' (${it.id})")

                    if (it.protection.delete) {
                        logInfo("removing delete protection from volume '${it.name}' (${it.id})")
                        val action = api.volumes.changeDeleteProtection(it.id, false)
                        api.volumes.waitForAction(action)
                    }

                    if (it.server != null) {
                        api.waitForAction(
                            {
                                logInfo("detaching volume ${it.name} from server ${it.server}")
                                api.volumes.detach(it.id)
                            },
                            { api.volumes.action(it) },
                        )
                    }

                    api.volumes.delete(it.id)
                } catch (e: HetznerApiException) {
                    if (e.error.code == HetznerApiErrorType.LOCKED) {
                        logWarning("skipping locked volume ${it.name}")
                    }
                }
            }
            api.servers
                .list(
                    labelSelectors = defaultLabels.toLabelSelectors(),
                )
                .forEach {
                    logInfo("cleaning up server '${it.name}' (${it.id})")
                    api.servers.delete(it.id)
                }
        }
    }

    data class Volume(val id: Long, val linuxDevice: String)

    fun createVolume(name: String? = null, location: HetznerLocation? = null): Volume {
        val resourceName = name ?: testId

        logInfo("creating volume '$resourceName'")
        return runBlocking {
            api.volumes
                .create(
                    VolumeCreateRequest(
                        resourceName,
                        16,
                        location ?: defaultLocation,
                        VolumeFormat.ext4,
                        labels = defaultLabels,
                        automount = false,
                    ),
                )
                .volume
                .let { Volume(it.id, it.linuxDevice) }
        }
    }

    fun destroyServer(server: HetznerServerTestContext) {
        runBlocking {
            api.waitForAction(
                {
                    logInfo("shutting down server '${server.id}'")
                    api.servers.shutdown(server.id)
                },
                {
                    logInfo("waiting for server '${server.id}' shutdown")
                    api.servers.action(it)
                },
            )
            api.waitForAction(
                {
                    logInfo("deleting server '${server.id}'")
                    api.servers.delete(server.id)
                },
                {
                    logInfo("waiting for server '${server.id}' deletion")
                    api.servers.action(it)
                },
            )
        }
    }

    fun destroyVolume(volume: Volume) = runBlocking { api.volumes.delete(volume.id) }
}
