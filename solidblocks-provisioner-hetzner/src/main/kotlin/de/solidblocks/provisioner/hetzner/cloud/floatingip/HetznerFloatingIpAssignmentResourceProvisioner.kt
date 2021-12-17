package de.solidblocks.provisioner.hetzner.cloud.floatingip

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.base.Waiter
import de.solidblocks.core.Result
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.AssignFloatingIPRequest
import mu.KotlinLogging

class HetznerFloatingIpAssignmentResourceProvisioner(
    hetznerCloudAPI: HetznerCloudAPI,
    val provisioner: InfrastructureProvisioner
) :
    IResourceLookupProvider<IFloatingIpAssignmentLookup, FloatingIpAssignmentRuntime>,
    IInfrastructureResourceProvisioner<FloatingIpAssignment, FloatingIpAssignmentRuntime>,
    BaseHetznerProvisioner<FloatingIpAssignment, FloatingIpAssignmentRuntime, HetznerCloudAPI>(hetznerCloudAPI) {

    private val logger = KotlinLogging.logger {}

    override fun destroyAll(): Boolean {
        logger.info { "unassigning all floating ips" }

        return Waiter.defaultWaiter().waitForResult {
            checkedApiCall {
                it.floatingIPs.floatingIPs
            }
        }.mapSuccessNonNullBoolean {
            it.map { floatingIp ->
                logger.info { "unassigning floating ip '${floatingIp.name}'" }
                destroy(floatingIp.id)
            }.all { it }
        }
    }

    override fun diff(resource: FloatingIpAssignment): Result<ResourceDiff> {
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

                checkedApiCall {
                    it.assignFloatingIP(floatingIp.id.toLong(), request.build())
                }.mapNonNullResult {
                    waitForActions(
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

    override fun destroy(resource: FloatingIpAssignment): Boolean {
        return lookup(resource).mapSuccessNonNullBoolean {
            destroy(it.id.toLong())
        }
    }

    private fun destroy(id: Long): Boolean {
        return checkedApiCall {
            it.getFloatingIP(id)
        }.mapSuccessNonNullBoolean {

            if (it.floatingIP.server == null) {
                return@mapSuccessNonNullBoolean true
            }

            checkedApiCall {
                it.unassignFloatingIP(id)
            }.mapSuccessNonNullBoolean {
                waitForActions(
                    listOf(it.action).filterNotNull()
                ) { api, action ->
                    val actionResult = api.getActionOfFloatingIP(id, action.id).action
                    logger.info { "waiting for unassign action '${action.command}' to finish for floating ip '$id', current status is '${action.status}'" }
                    actionResult.finished != null
                }.success()
            }
        }
    }

    override fun getResourceType(): Class<*> {
        return FloatingIpAssignment::class.java
    }

    override fun lookup(lookup: IFloatingIpAssignmentLookup): Result<FloatingIpAssignmentRuntime> {
        return provisioner.lookup(lookup.floatingIp()).mapNonNullResult {
            FloatingIpAssignmentRuntime(it.id, it.server)
        }
    }

    override fun getLookupType(): Class<*> {
        return IFloatingIpAssignmentLookup::class.java
    }
}