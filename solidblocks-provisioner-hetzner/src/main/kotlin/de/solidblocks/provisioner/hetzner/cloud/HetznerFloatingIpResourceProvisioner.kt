package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.api.resources.infrastructure.network.FloatingIpRuntime
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.FloatingIPRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerFloatingIpResourceProvisioner(credentialsProvider: HetznerCloudCredentialsProvider) :
    BaseHetznerProvisioner<FloatingIp, FloatingIpRuntime, HetznerCloudAPI>(
        { HetznerCloudAPI(credentialsProvider.defaultApiToken()) },
        FloatingIp::class.java
    ) {

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

    override fun diff(resource: FloatingIp): Result<ResourceDiff<FloatingIpRuntime>> {
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

    override fun lookup(resource: FloatingIp): Result<FloatingIpRuntime> {
        return checkedApiCall(resource, HetznerCloudAPI::getFloatingIPs) {
            it.floatingIPs.floatingIPs.firstOrNull { it.name == resource.name }
        }.mapNonNullResult {
            FloatingIpRuntime(it.id.toString(), it.ip, it.server)
        }
    }
}
