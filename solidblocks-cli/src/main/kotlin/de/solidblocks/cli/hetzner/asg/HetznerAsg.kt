package de.solidblocks.cli.hetzner.asg

import com.github.ajalt.clikt.core.CliktError
import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerHealthStatusResponse
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType.ip
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType.label_selector
import de.solidblocks.cli.utils.logInfo
import kotlinx.coroutines.runBlocking

class HetznerAsg(val hcloudToken: String) {

    val api = HetznerApi(hcloudToken)

    suspend fun ensureLoadBalancer(loadbalancer: LoadBalancerReference) = when (loadbalancer) {
        is LoadBalancerReference.LoadBalancerId -> api.loadBalancers.get(loadbalancer.id)?.loadbalancer
            ?: throw CliktError("load balancer with id ${loadbalancer.id} not found.")

        is LoadBalancerReference.LoadBalancerName -> api.loadBalancers.get(loadbalancer.name)?.loadbalancer
            ?: throw CliktError("load balancer with name ${loadbalancer.name} not found.")
    }

    fun run(loadbalancer: LoadBalancerReference, replicas: Int = 1) {
        runBlocking {

            val loadbalancer = ensureLoadBalancer(loadbalancer)

            if (loadbalancer.targets.any { it.type == ip }) {
                throw CliktError("ip based targets are not supported for server rotation")
            }

            logInfo("inspecting load balancer '${loadbalancer.name}'")

            val directAttachedServersTargetGroup =
                loadbalancer.targets.filter { it.type == LoadBalancerTargetType.server && it.server != null }

            data class ServerInfo(val name: String, val status: List<LoadBalancerHealthStatusResponse>)

            val directAttachedServers =
                directAttachedServersTargetGroup.filter { it.server != null }.mapNotNull {
                    val server = api.servers.get(it.server!!.id)
                    if (server != null) {
                        ServerInfo(server.server.name, it.status)
                    } else {
                        null
                    }
                }

            if (directAttachedServers.isNotEmpty()) {
                logInfo("found ${directAttachedServers.size} attached server(s) (${directAttachedServers.joinToString(", ") { "'${it.name}'" }}) on load balancer '${loadbalancer.name}'")
            }

            val labelSelectors =
                loadbalancer.targets.filter { it.type == label_selector && it.labelSelector != null && it.targets != null }

            val labelMatchedServers = labelSelectors.flatMap {
                val tmp = it.targets!!.mapNotNull {
                    val server = api.servers.get(it.server.id)
                    if (server != null) {
                        ServerInfo(server.server.name, it.status)
                    } else {
                        null
                    }
                }

                if (tmp.isNotEmpty()) {
                    logInfo("found ${tmp.size} servers (${tmp.joinToString(", ") { "'${it.name}'" }}) on load balancer '${loadbalancer.name}' matching label selector '${it.labelSelector?.selector ?: "<unknown>"}'")
                }

                tmp
            }

            if (labelMatchedServers.isEmpty() && directAttachedServers.isEmpty()) {
                logInfo("no servers currently attached to load balancer '${loadbalancer.name}'")
            } else {
                logInfo("inspecting servers")

                (labelMatchedServers + directAttachedServers).forEach {
                    logInfo("server '${it.name}' (${it.status.joinToString { "port ${it.listenPort}: ${it.status}" }})")
                }
            }
        }
    }
}
