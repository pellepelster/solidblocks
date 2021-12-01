package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.compute.IServerLookup
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.compute.ServerRuntime
import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.Constants
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import de.solidblocks.provisioner.Provisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.ServerRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerServerResourceProvisioner(
        private val provisioner: Provisioner,
        cloudContext: CloudConfigurationContext
) :
        IResourceLookupProvider<IServerLookup, ServerRuntime>,
        IInfrastructureResourceProvisioner<Server,
                ServerRuntime>,
        BaseHetznerProvisioner<Server, ServerRuntime, HetznerCloudAPI>(
                { HetznerCloudAPI(cloudContext.configurationValue(Constants.ConfigKeys.HETZNER_CLOUD_API_TOKEN_RW_KEY)) }) {

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
            return sshKeys.reduceResults()
        }

        sshKeys.forEach {
            logger.info { "adding sshkey '${it.resource.id()}' to server '${resource.id}'" }
        }
        request.sshKeys(sshKeys.map { it.result!!.id.toLong() })

        return checkedApiCall(resource, HetznerCloudAPI::createServer) {
            logger.info { "creating server '${resource.id}'" }
            it.createServer(request.build())
        }.mapNonNullResult {
            waitForActions(resource, HetznerCloudAPI::getActionOfServer, it.nextActions) { api, action ->
                val actionResult = api.getActionOfServer(it.server.id, action.id).action
                logger.info { "waiting for action '${action.command}' to finish for server '${it.server.name}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun apply(resource: Server): Result<*> {
        return lookup(resource).mapNullResult {
            createServer(resource)
        }.mapNonNullResult {
            this.lookup(resource).mapNonNullResult { serverRuntime ->
                waitForApiCall(resource) {
                    val server = it.getServerById(serverRuntime.id.toLong()).server
                    "initializing" != server.status
                }

                checkedApiCall(resource, HetznerCloudAPI::powerOnServer) {
                    it.powerOnServer(serverRuntime.id.toLong())
                }.mapNonNullResult {
                    waitForActions(resource, HetznerCloudAPI::getActionOfServer, listOf(it.action)) { api, action ->
                        val actionResult = api.getActionOfServer(serverRuntime.id.toLong(), action.id).action
                        logger.info { "waiting for action '${action.command}' to finish for server '${resource.id}', current status is '${action.status}'" }
                        actionResult.finished != null
                    }
                }
            }
        }
    }

    override fun destroy(resource: Server): Result<*> {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }
    }

    private fun destroy(id: Long): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::deleteServer) {
            it.deleteServer(id)
        }
    }

    override fun destroyAll(): Result<*> {
        logger.info { "destroying all servers" }
        return checkedApiCall(NullResource, HetznerCloudAPI::getServers) {
            it.servers.servers
        }.mapNonNullResult {
            it.map { server ->
                logger.info { "destroying server '${server.name}'" }
                destroy(server.id)
            }.reduceResults()
        }
    }

    override fun getResourceType(): Class<*> {
        return Server::class.java
    }

    override fun lookup(lookup: IServerLookup): Result<ServerRuntime> {
        return checkedApiCall(lookup, HetznerCloudAPI::getServers) {
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
