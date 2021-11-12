package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.network.FloatingIpAssignment
import de.solidblocks.api.resources.infrastructure.network.FloatingIpAssignmentRuntime
import de.solidblocks.base.Waiter
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import de.solidblocks.provisioner.Provisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.AssignFloatingIPRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerFloatingIpAssignmentResourceProvisioner(
    val provisioner: Provisioner,
    credentialsProvider: HetznerCloudCredentialsProvider
) :
    BaseHetznerProvisioner<FloatingIpAssignment, FloatingIpAssignmentRuntime, HetznerCloudAPI>(
        { HetznerCloudAPI(credentialsProvider.defaultApiToken()) },
        FloatingIpAssignment::class.java
    ) {

    private val logger = KotlinLogging.logger {}

    override fun destroyAll(): Result<*> {
        logger.info { "unassigning all floating ips" }

        return Waiter.defaultWaiter().waitFor {
            checkedApiCall(NullResource, HetznerCloudAPI::getFloatingIPs) {
                it.floatingIPs.floatingIPs
            }
        }.mapNonNullResult {
            it.map { floatingIp ->
                logger.info { "unassigning floating ip '${floatingIp.name}'" }
                destroy(floatingIp.id)
            }.reduceResults()
        }
    }

    override fun diff(resource: FloatingIpAssignment): Result<ResourceDiff<FloatingIpAssignmentRuntime>> {
        return lookup(resource).mapResourceResultOrElse(
            {
                if (it.server != null) {
                    ResourceDiff(resource)
                } else {
                    ResourceDiff(
                        resource,
                        changes = listOf(ResourceDiffItem("server", missing = true))
                    )
                }
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: FloatingIpAssignment): Result<*> {
        return this.provisioner.lookup(resource.floatingIp).mapNonNullResult { floatingIp ->
            this.provisioner.lookup(resource.server).mapNonNullResult { server ->

                val request = AssignFloatingIPRequest.builder()
                request.serverID(server.id.toLong())

                checkedApiCall(resource, HetznerCloudAPI::assignFloatingIP) {
                    it.assignFloatingIP(floatingIp.id.toLong(), request.build())
                }.mapNonNullResult {
                    waitForActions(
                        resource,
                        HetznerCloudAPI::getActionOfFloatingIP,
                        listOf(it.action).filterNotNull()
                    ) { api, action ->
                        val actionResult = api.getActionOfFloatingIP(floatingIp.id.toLong(), action.id).action
                        logger.info { "waiting for action '${action.command}' to finish for floating ip '${floatingIp.id.toLong()}', current status is '${action.status}'" }
                        actionResult.finished != null
                    }
                }
            }
        }
    }

    override fun destroy(resource: FloatingIpAssignment): Result<*> {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }
    }

    private fun destroy(id: Long): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::getFloatingIP) {
            it.getFloatingIP(id)
        }.mapNonNullResult {

            if (it.floatingIP.server == null) {
                return@mapNonNullResult it
            }

            checkedApiCall(NullResource, HetznerCloudAPI::unassignFloatingIP) {
                it.unassignFloatingIP(id)
            }.mapNonNullResult {
                waitForActions(
                    NullResource,
                    HetznerCloudAPI::getActionOfFloatingIP,
                    listOf(it.action).filterNotNull()
                ) { api, action ->
                    val actionResult = api.getActionOfFloatingIP(id, action.id).action
                    logger.info { "waiting for unassign action '${action.command}' to finish for floating ip '$id', current status is '${action.status}'" }
                    actionResult.finished != null
                }
            }
        }
    }

    override fun lookup(resource: FloatingIpAssignment): Result<FloatingIpAssignmentRuntime> {
        return provisioner.lookup(resource.floatingIp).mapNonNullResultNullable { floatingIp ->
            FloatingIpAssignmentRuntime(floatingIp.id, floatingIp.server)
        }
    }
}
