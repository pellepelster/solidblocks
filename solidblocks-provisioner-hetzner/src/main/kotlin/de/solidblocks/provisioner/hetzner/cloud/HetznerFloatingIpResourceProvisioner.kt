package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.api.resources.infrastructure.network.FloatingIpRuntime
import de.solidblocks.api.resources.infrastructure.network.IFloatingIpLookup
import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.Constants
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.FloatingIPRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerFloatingIpResourceProvisioner(cloudContext: CloudConfigurationContext) :
    IResourceLookupProvider<IFloatingIpLookup, FloatingIpRuntime>,
    IInfrastructureResourceProvisioner<FloatingIp, FloatingIpRuntime>,
    BaseHetznerProvisioner<FloatingIp, FloatingIpRuntime, HetznerCloudAPI>(
        { HetznerCloudAPI(cloudContext.configurationValue(Constants.ConfigKeys.HETZNER_CLOUD_API_TOKEN_RW_KEY)) }) {

    private val logger = KotlinLogging.logger {}

    override fun destroyAll(): Result<*> {
        logger.info { "destroying all floating ips" }

        return checkedApiCall(NullResource, HetznerCloudAPI::getFloatingIPs) {
            it.floatingIPs.floatingIPs
        }.mapNonNullResult {
            it.map {
                logger.info { "destroying floating ip '${it.name}'" }
                destroy(it.id)
            }.reduceResults()
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
        request.name(resource.name)
        request.homeLocation(resource.location)
        request.type("ipv4")
        request.labels(resource.labels)

        return checkedApiCall(resource, HetznerCloudAPI::createFloatingIP) {
            it.createFloatingIP(request.build())
        }.mapNonNullResult {
            waitForActions(
                resource,
                HetznerCloudAPI::getActionOfFloatingIP,
                listOf(it.action).filterNotNull()
            ) { api, action ->
                val actionResult = api.getActionOfFloatingIP(it.floatingIP.id, action.id).action
                logger.info { "waiting for action '${action.command}' to finish for floating ip '${it.floatingIP.name}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun destroy(resource: FloatingIp): Result<*> {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }
    }

    private fun destroy(id: Long): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::deleteFloatingIP) {
            it.deleteFloatingIP(id)
        }
    }

    override fun getResourceType(): Class<*> {
        return FloatingIp::class.java
    }

    override fun lookup(lookup: IFloatingIpLookup): Result<FloatingIpRuntime> {
        return checkedApiCall(lookup, HetznerCloudAPI::getFloatingIPs) {
            it.floatingIPs.floatingIPs.firstOrNull { it.name == lookup.name() }
        }.mapNonNullResult {
            FloatingIpRuntime(it.id.toString(), it.ip, it.server)
        }
    }

    override fun getLookupType(): Class<*> {
        return IFloatingIpLookup::class.java
    }
}
