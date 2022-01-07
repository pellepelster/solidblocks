package de.solidblocks.provisioner.hetzner.cloud.floatingip

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.FloatingIPRequest
import mu.KotlinLogging

class HetznerFloatingIpResourceProvisioner(hetznerCloudAPI: HetznerCloudAPI) :
    IResourceLookupProvider<IFloatingIpLookup, FloatingIpRuntime>,
    IInfrastructureResourceProvisioner<FloatingIp, FloatingIpRuntime>,
    BaseHetznerProvisioner<FloatingIp, FloatingIpRuntime, HetznerCloudAPI>(hetznerCloudAPI) {

    private val logger = KotlinLogging.logger {}

    override fun destroyAll(): Boolean {
        logger.info { "destroying all floating ips" }

        return checkedApiCall {
            it.floatingIPs.floatingIPs
        }.mapSuccessNonNullBoolean {
            it.map {
                logger.info { "destroying floating ip '${it.name}'" }
                destroy(it.id)
            }.ifEmpty { listOf(true) }.all { it }.also {
                if (!it) {
                    logger.error { "destroying all floating ips failed" }
                }
            }
        }
    }

    override fun diff(resource: FloatingIp): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, missing = false)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: FloatingIp): Result<*> {
        val request = FloatingIPRequest.builder()
        request.name(resource.id)
        request.homeLocation(resource.location)
        request.type("ipv4")
        request.labels(resource.labels)

        return checkedApiCall {
            it.createFloatingIP(request.build())
        }.mapNonNullResult {
            waitForActions(
                listOf(it.action).filterNotNull()
            ) { api, action ->
                val actionResult = api.getActionOfFloatingIP(it.floatingIP.id, action.id).action
                logger.info { "waiting for action '${action.command}' to finish for floating ip '${it.floatingIP.name}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun destroy(resource: FloatingIp): Boolean {
        return lookup(resource).mapSuccessNonNullBoolean {
            destroy(it.id.toLong())
        }
    }

    private fun destroy(id: Long): Boolean {
        return checkedApiCall {
            it.deleteFloatingIP(id)
        }.mapSuccessBoolean()
    }

    override fun getResourceType(): Class<*> {
        return FloatingIp::class.java
    }

    override fun lookup(lookup: IFloatingIpLookup): Result<FloatingIpRuntime> {
        return checkedApiCall {
            it.floatingIPs.floatingIPs.firstOrNull { it.name == lookup.id() }
        }.mapNonNullResult {
            FloatingIpRuntime(it.id.toString(), it.ip, it.server)
        }
    }

    override fun getLookupType(): Class<*> {
        return IFloatingIpLookup::class.java
    }
}
