package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.compute.IServerLookup
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.compute.ServerRuntime
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.ServerRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerServerResourceProvisioner(
        hetznerCloudAPI: HetznerCloudAPI,
        private val provisioner: Provisioner,
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
                        resource.sshKeys.joinToString { "${it.id()}" }
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
                            resource::userData.name,
                            userData.result!!
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

    fun createServer(resource: Server): Result<*> {
        val request = ServerRequest.builder()

        request.name(resource.id)
        request.serverType("cx11")
        request.location(resource.location)
        request.image("debian-10")
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
        if (userData.isEmptyOrFailed()) return userData

        request.userData(userData.result)

        val labels = HetznerLabels()
        labels.addHashLabel(resource::sshKeys.name, resource.sshKeys.joinToString { it.id() })
        labels.addHashLabel(resource::userData.name, userData.result!!)

        request.labels(labels.labels() + resource.labels)

        val sshKeys = resource.sshKeys.map { this.provisioner.lookup(it) }

        if (sshKeys.any { it.failed }) {
            return Result<Any>(failed = true)
        }

        request.sshKeys(sshKeys.map { it.result!!.id.toLong() })

        return checkedApiCall(HetznerCloudAPI::createServer) {
            logger.info { "creating server '${resource.id}'" }
            it.createServer(request.build())
        }.mapNonNullResult {
            waitForActions(HetznerCloudAPI::getActionOfServer, it.nextActions) { api, action ->
                val actionResult = api.getActionOfServer(it.server.id, action.id).action
                logger.info { "waiting for action '${action.command}' to finish for server '${it.server.name}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun apply(resource: Server): Result<*> {
        val lookup = lookup(resource)

        if (!lookup.isEmpty()) {
            TODO("update not implemented")
        }

        return createServer(resource).mapNonNullResult {
            this.lookup(resource).mapNonNullResult { serverRuntime ->
                waitForApiCall(resource) {
                    val server = it.getServerById(serverRuntime.id.toLong()).server
                    "initializing" != server.status
                }

                checkedApiCall(HetznerCloudAPI::powerOnServer) {
                    it.powerOnServer(serverRuntime.id.toLong())
                }.mapNonNullResult {
                    waitForActions(HetznerCloudAPI::getActionOfServer, listOf(it.action)) { api, action ->
                        val actionResult = api.getActionOfServer(serverRuntime.id.toLong(), action.id).action
                        logger.info { "waiting for action '${action.command}' to finish for server '${resource.id}', current status is '${action.status}'" }
                        actionResult.finished != null
                    }
                }
            }
        }
    }

    override fun destroy(resource: Server): Boolean {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }.mapSuccessNonNullBoolean { true }
    }

    private fun destroy(id: Long): Boolean {
        return checkedApiCall(HetznerCloudAPI::deleteServer) {
            it.deleteServer(id)
        }.mapSuccessNonNullBoolean { true }
    }

    override fun destroyAll(): Boolean {
        logger.info { "destroying all servers" }
        return checkedApiCall(HetznerCloudAPI::getServers) {
            it.servers.servers
        }.mapSuccessNonNullBoolean {
            it.map { server ->
                logger.info { "destroying server '${server.name}'" }
                destroy(server.id)
            }.any { it }
        }
    }

    override fun getResourceType(): Class<*> {
        return Server::class.java
    }

    override fun lookup(lookup: IServerLookup): Result<ServerRuntime> {
        return checkedApiCall(HetznerCloudAPI::getServers) {
            it.servers.servers.firstOrNull {
                it.name == lookup.id()
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

    override fun getLookupType(): Class<*> {
        return IServerLookup::class.java
    }
}
