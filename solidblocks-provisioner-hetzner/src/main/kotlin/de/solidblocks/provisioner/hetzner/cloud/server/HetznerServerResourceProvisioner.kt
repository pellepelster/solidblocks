package de.solidblocks.provisioner.hetzner.cloud.server

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.core.Result
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.provisioner.hetzner.cloud.HetznerLabels
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.ServerRequest
import mu.KotlinLogging

class HetznerServerResourceProvisioner(
    hetznerCloudAPI: HetznerCloudAPI,
    val provisioner: InfrastructureProvisioner
) :
    IResourceLookupProvider<IServerLookup, ServerRuntime>,
    IInfrastructureResourceProvisioner<Server,
        ServerRuntime>,
    BaseHetznerProvisioner<Server, ServerRuntime, HetznerCloudAPI>(hetznerCloudAPI) {

    private val logger = KotlinLogging.logger {}

    override fun diff(resource: Server): Result<ResourceDiff> {
        return this.lookup(resource).mapResourceResultOrElse(
            {

                val labels = HetznerLabels(it.labels)
                val changes = ArrayList<ResourceDiffItem>()

                if (!labels.hashLabelMatches(
                        resource::sshKeys.name,
                        resource.sshKeys.joinToString { it.name }
                    )
                ) {
                    changes.add(
                        ResourceDiffItem(
                            resource::sshKeys,
                            triggersRecreate = true,
                            changed = true
                        )
                    )
                }

                val userData = provisioner.lookup(resource.userData)

                if (userData.result != null) {
                    if (!labels.hashLabelMatches(
                            STATIC_USER_DATA_KEY,
                            userData.result!!.staticUserData
                        )
                    ) {
                        changes.add(
                            ResourceDiffItem(
                                resource::userData,
                                triggersRecreate = true,
                                changed = true
                            )
                        )
                    }
                }

                if (resource.volume != null && !it.hasVolumes) {
                    changes.add(
                        ResourceDiffItem(
                            resource::volume,
                            changed = true
                        )
                    )
                }

                if ("running" != it.status) {
                    changes.add(
                        ResourceDiffItem(
                            "status",
                            changed = true
                        )
                    )
                }

                ResourceDiff(resource, changes = changes)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    private val STATIC_USER_DATA_KEY = "staticUserData"

    private val EPHEMERAL_USER_DATA_KEY = "ephemeralUserData"

    fun createServer(resource: Server): Result<*> {
        val request = ServerRequest.builder()

        request.name(resource.name)
        request.serverType("cx11")
        request.location(resource.location)
        request.image("debian-11")
        request.startAfterCreate(false)

        if (resource.volume != null) {
            val volumeResult = provisioner.lookup(resource.volume!!)
            if (volumeResult.isEmptyOrFailed()) {
                return volumeResult
            }

            request.volume(volumeResult.result?.id?.toLong())
        }

        val network = this.provisioner.lookup(resource.network)
        if (network.isEmptyOrFailed()) return network
        request.network(network.result?.id?.toLong())

        val userData = provisioner.lookup(resource.userData)
        if (userData.isEmptyOrFailed()) {
            logger.error { "user data retrieval failed" }
            return userData
        }

        request.userData(userData.result!!.ephemeralUserData)

        val labels = HetznerLabels()
        labels.addHashLabel(resource::sshKeys.name, resource.sshKeys.joinToString { it.name })
        labels.addHashLabel(EPHEMERAL_USER_DATA_KEY, userData.result!!.ephemeralUserData)
        labels.addHashLabel(STATIC_USER_DATA_KEY, userData.result!!.staticUserData)

        request.labels(labels.labels() + resource.labels)

        val sshKeys = resource.sshKeys.map { this.provisioner.lookup(it) }

        if (sshKeys.any { it.failed }) {
            return Result<Any>(failed = true)
        }

        request.sshKeys(sshKeys.map { it.result!!.id.toLong() })

        return checkedApiCall {
            logger.info { "creating server '${resource.name}' with ssh keys ${sshKeys.joinToString(", ") { it.result!!.id }}" }
            it.createServer(request.build())
        }.mapNonNullResult {
            waitForActions(it.nextActions) { api, action ->
                val actionResult = api.getActionOfServer(it.server.id, action.id).action
                logger.info { "waiting for action '${action.command}' to finish for server '${it.server.name}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun apply(resource: Server): Result<*> {
        var existingServer = lookup(resource)

        if (existingServer.isEmpty()) {
            logger.info { "server '${resource.name}' not found, creating" }
            val result = createServer(resource)

            if (result.failed) {
                logger.info { "creating server '${resource.name}' failed" }
                return result
            }

            existingServer = lookup(resource)
        }

        if (existingServer.isEmpty()) {
            logger.error { "server creation failed" }
            return Result<Server>(failed = true)
        }

        waitForRetryableApiCall {
            val server = it.getServerById(existingServer.result!!.id.toLong()).server
            logger.info { "waiting for server initialization to finish, current status is '${server.status}'" }
            "initializing" != server.status
        }

        return checkedApiCall {
            logger.info { "powering on server '${resource.name}'" }
            it.powerOnServer(existingServer.result!!.id.toLong())
        }.mapNonNullResult {
            waitForActions(listOf(it.action)) { api, action ->
                val actionResult = api.getActionOfServer(existingServer.result!!.id.toLong(), action.id).action
                logger.info { "waiting for action '${action.command}' to finish for server '${resource.name}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun destroy(resource: Server): Boolean {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }.mapSuccessNonNullBoolean { true }
    }

    private fun destroy(id: Long): Boolean {
        return checkedApiCall {
            it.deleteServer(id)
        }.mapSuccessNonNullBoolean { true }
    }

    override fun destroyAll(): Boolean {
        logger.info { "destroying all servers" }
        return checkedApiCall {
            it.servers.servers
        }.mapSuccessNonNullBoolean {
            it.map { server ->
                logger.info { "destroying server '${server.name}'" }
                destroy(server.id)
            }.ifEmpty { listOf(true) }.all { it }.also {
                if (!it) {
                    logger.error { "destroying all servers failed" }
                }
            }
        }
    }

    override fun lookup(lookup: IServerLookup): Result<ServerRuntime> {
        return checkedApiCall {
            it.servers.servers.firstOrNull {
                it.name == lookup.name
            }
        }.mapNonNullResult {
            ServerRuntime(
                it.id.toString(),
                it.status,
                it.labels,
                it.volumes.isNotEmpty(),
                it.privateNet.firstOrNull()?.ip,
                it.publicNet?.ipv4?.ip
            )
        }
    }

    override val resourceType = Server::class.java

    override val lookupType = IServerLookup::class.java
}
