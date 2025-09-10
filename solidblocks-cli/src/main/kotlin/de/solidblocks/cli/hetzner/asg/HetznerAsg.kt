package de.solidblocks.cli.hetzner.asg

import com.github.ajalt.clikt.core.CliktError
import de.solidblocks.cli.hetzner.HetznerLabels
import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerHealthStatusResponse
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType.ip
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType.label_selector
import de.solidblocks.cli.hetzner.hashString
import de.solidblocks.cli.utils.logInfo
import kotlinx.coroutines.runBlocking

enum class ServerInfoAttachmentType { direct, label_selector }

data class ServerInfo(
    val name: String,
    val status: List<LoadBalancerHealthStatusResponse>,
    val attachment: ServerInfoAttachmentType,
    val labels: Map<String, String>,
    val selector: String? = null
) {
    fun isUpToDate(userDataHash: String) =
        HetznerLabels(labels).hashLabelMatches("blcks.de/user-data-hash", userDataHash)
}

class HetznerAsg(hcloudToken: String) {

    val api = HetznerApi(hcloudToken)

    suspend fun serverInfo(id: Long): List<ServerInfo> {
        val loadbalancer =
            api.loadBalancers.get(id)?.loadbalancer ?: throw RuntimeException("loadbalancer '$id' not found")

        return loadbalancer.targets.flatMap { lbTarget ->

            if (lbTarget.type == LoadBalancerTargetType.server && lbTarget.server != null) {
                val server = api.servers.get(lbTarget.server.id)
                if (server != null) {
                    return@flatMap listOf(
                        ServerInfo(
                            server.server.name,
                            lbTarget.status,
                            ServerInfoAttachmentType.direct,
                            labels = server.server.labels,
                        )
                    )
                } else {
                    return@flatMap emptyList()
                }
            }

            if (lbTarget.type == label_selector && lbTarget.labelSelector != null && lbTarget.targets != null) {
                return lbTarget.targets.mapNotNull {
                    val server = api.servers.get(it.server.id)
                    if (server != null) {
                        ServerInfo(
                            server.server.name,
                            it.status,
                            ServerInfoAttachmentType.label_selector,
                            labels = server.server.labels,
                            lbTarget.labelSelector.selector
                        )
                    } else {
                        null
                    }
                }

            }

            emptyList()
        }


    }

    suspend fun ensureLoadBalancer(loadbalancer: LoadBalancerReference) = when (loadbalancer) {
        is LoadBalancerReference.LoadBalancerId -> api.loadBalancers.get(loadbalancer.id)?.loadbalancer
            ?: throw CliktError("load balancer with id ${loadbalancer.id} not found.")

        is LoadBalancerReference.LoadBalancerName -> api.loadBalancers.get(loadbalancer.name)?.loadbalancer
            ?: throw CliktError("load balancer with name ${loadbalancer.name} not found.")
    }

    fun run(loadbalancer: LoadBalancerReference, replicas: Int = 1, userData: String) {
        runBlocking {

            val loadbalancer = ensureLoadBalancer(loadbalancer)

            if (loadbalancer.targets.any { it.type == ip }) {
                throw CliktError("ip based targets are not supported for server rotation")
            }

            val userDataHash = hashString(userData)

            logInfo("useer data hash for new servers is '${userDataHash}'")
            logInfo("inspecting load balancer '${loadbalancer.name}'")

            val servers = serverInfo(loadbalancer.id)
            servers.filter { it.attachment == ServerInfoAttachmentType.direct }.let {
                if (it.isNotEmpty()) {
                    logInfo("found ${it.size} attached server(s) (${it.joinToString(", ") { "'${it.name}'" }}) on load balancer '${loadbalancer.name}'")
                }
            }

            servers.filter { it.attachment == ServerInfoAttachmentType.label_selector }.let {
                if (it.isNotEmpty()) {
                    logInfo("found ${it.size} servers (${it.joinToString(", ") { "'${it.name}'" }}) on load balancer '${loadbalancer.name}' matching label selector '${it.first().selector ?: "<unknown>"}'")
                }
            }

            if (servers.isEmpty()) {
                logInfo("no servers currently attached to load balancer '${loadbalancer.name}'")
            } else {
                logInfo("inspecting servers for  load balancer '${loadbalancer.name}'")

                servers.forEach {

                    if (it.isUpToDate(userDataHash).matches) {
                        logInfo("server '${it.name}' (${it.status.joinToString { "port ${it.listenPort}: ${it.status}" }}) is up to date")
                    } else {
                        logInfo("server '${it.name}' (${it.status.joinToString { "port ${it.listenPort}: ${it.status}" }}) needs to be rotated")
                    }
                }
            }
        }
    }
}
