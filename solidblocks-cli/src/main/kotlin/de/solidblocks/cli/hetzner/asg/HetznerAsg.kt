package de.solidblocks.cli.hetzner.asg

import com.github.ajalt.clikt.core.CliktError
import de.solidblocks.cli.hetzner.AlphanumericHasher
import de.solidblocks.cli.hetzner.Constants.deploymentIdLabel
import de.solidblocks.cli.hetzner.Constants.loadBalancerIdLabel
import de.solidblocks.cli.hetzner.Constants.managedByLabel
import de.solidblocks.cli.hetzner.Constants.userDataHashLabel
import de.solidblocks.cli.hetzner.HetznerLabels
import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerApiErrorType
import de.solidblocks.cli.hetzner.api.HetznerApiException
import de.solidblocks.cli.hetzner.api.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.resources.*
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType.ip
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType.label_selector
import de.solidblocks.cli.hetzner.hashString
import de.solidblocks.cli.utils.logError
import de.solidblocks.cli.utils.logInfo
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class ServerInfoAttachmentType { direct, label_selector }

data class LoadbalancerServer @OptIn(ExperimentalTime::class) constructor(
    val name: String,
    val id: Long,
    private val lbStatus: List<LoadBalancerHealthStatusResponse>,
    val attachment: ServerInfoAttachmentType,
    val created: Instant,
    val labels: Map<String, String>,
    val selector: String? = null,
) {
    fun isUpToDate(userDataHash: String) =
        HetznerLabels(labels).labelMatches(userDataHashLabel, userDataHash)

    @OptIn(ExperimentalTime::class)
    val age: Duration
        get() = Clock.System.now().minus(created)

    val status: LoadBalancerHealthStatus
        get() = if (age < 30.seconds) {
            LoadBalancerHealthStatus.unhealthy
        } else if (lbStatus.all { it.status == LoadBalancerHealthStatus.healthy }) {
            LoadBalancerHealthStatus.healthy
        } else if (lbStatus.all { it.status == LoadBalancerHealthStatus.unhealthy }) {
            LoadBalancerHealthStatus.unhealthy
        } else {
            LoadBalancerHealthStatus.unknown
        }

    val statusLogText: String
        get() = lbStatus.joinToString { "port ${it.listenPort}: ${it.status}" }
}

fun List<LoadbalancerServer>.statusLogText() = this.joinToString(", ") { "'${it.name}' (age: ${it.age})" }

enum class ASG_ROTATE_STATUS { OK, TIMEOUT, LOADBALANCER_NOT_FOUND, LOCATION_NOT_FOUND, SERVER_TYPE_NOT_FOUND, IMAGE_TYPE_NOT_FOUND }

class HetznerAsg(hcloudToken: String) {

    val api = HetznerApi(hcloudToken)

    @OptIn(ExperimentalTime::class)
    suspend fun fetchLoadbalancerServers(id: Long): List<LoadbalancerServer> {
        val loadbalancer =
            api.loadBalancers.get(id)?.loadbalancer ?: throw RuntimeException("loadbalancer '$id' not found")

        val list = mutableListOf<LoadbalancerServer>()

        loadbalancer.targets.forEach { lbTarget ->
            if (lbTarget.type == LoadBalancerTargetType.server && lbTarget.server != null) {
                val server = api.servers.get(lbTarget.server.id)
                if (server != null) {
                    list.add(
                        LoadbalancerServer(
                            server.server.name,
                            server.server.id,
                            lbTarget.status,
                            ServerInfoAttachmentType.direct,
                            server.server.created,
                            labels = server.server.labels,
                        )
                    )
                }
            }

            if (lbTarget.type == label_selector && lbTarget.labelSelector != null && lbTarget.targets != null) {
                lbTarget.targets.forEach {
                    val server = api.servers.get(it.server.id)
                    if (server != null) {
                        list.add(
                            LoadbalancerServer(
                                server.server.name,
                                server.server.id,
                                it.status,
                                ServerInfoAttachmentType.label_selector,
                                server.server.created,
                                labels = server.server.labels,
                                lbTarget.labelSelector.selector
                            )
                        )
                    }
                }
            }
        }

        return list
    }

    suspend fun ensureLoadBalancer(loadbalancer: LoadBalancerReference): LoadBalancerResponse? {
        val lbByName = api.loadBalancers.get(loadbalancer.reference)?.loadbalancer
        val lbById = try {
            val id = loadbalancer.reference.toLong()
            api.loadBalancers.get(id)?.loadbalancer
        } catch (e: Exception) {
            null
        }

        if (lbById == null && lbByName == null) {
            logError("load balancer '${loadbalancer.reference}' not found.")
        }

        return lbByName ?: lbById
    }

    @OptIn(ExperimentalTime::class)
    fun run(
        loadbalancerRef: LoadBalancerReference,
        locationRef: LocationReference,
        serverTypeRef: ServerTypeReference,
        imageRef: ImageReference,
        userData: String,
        replicas: Int = 1,
        namePrefix: String? = null,
        sshKeyRefs: List<SSHKeyReference> = emptyList(),
        firewallRefs: List<FirewallReference> = emptyList(),
        placementGroupRef: PlacementGroupReference? = null,
        enableIpv4: Boolean = true,
        enableIpv6: Boolean = false,
        healthTimeout: Duration = 5.minutes,
        totalTimeout: Duration = 10.minutes
    ): ASG_ROTATE_STATUS = runBlocking {

        val deploymentStart = Clock.System.now()
        val loadbalancer =
            ensureLoadBalancer(loadbalancerRef) ?: return@runBlocking ASG_ROTATE_STATUS.LOADBALANCER_NOT_FOUND

        val networks = if (loadbalancer.privateNetworks.isNotEmpty()) {
            val networks = loadbalancer.privateNetworks.mapNotNull { api.networks.get(it.network) }
            logInfo(
                "load balancer '${loadbalancer.name}' is attached to private network(s) ${
                    networks.joinToString(
                        ", "
                    ) { "'${it.network.name}'" }
                }, new servers will be attached to those networks as well"
            )
            networks
        } else {
            logInfo("load balancer '${loadbalancer.name}' has no private network, will use public ip for new servers")
            emptyList()
        }

        if (loadbalancer.targets.any { it.type == ip }) {
            throw CliktError("ip based targets are not supported for server rotation")
        }

        val userDataHash = hashString(userData)

        logInfo("hash for provided user data is '${userDataHash}'")
        logInfo("inspecting load balancer '${loadbalancer.name}'")

        val servers = fetchLoadbalancerServers(loadbalancer.id)

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
        }

        var finished = false
        val deploymentId = AlphanumericHasher.hashToBase62(userDataHash, 12)
        val serverNamePrefix = "${namePrefix ?: loadbalancer.name}-${deploymentId}"
        val controlLoopDelay = 5.seconds

        logInfo("starting deployment with id '${deploymentId}', server name prefix is '${serverNamePrefix}' and timeout for newly created servers is '${healthTimeout}'")

        while (!finished) {

            if (Clock.System.now().minus(deploymentStart) > totalTimeout) {
                logError("rotation timeout of ${totalTimeout} reached, aborting")
                return@runBlocking ASG_ROTATE_STATUS.TIMEOUT
            }

            sleep(controlLoopDelay.inWholeMilliseconds)

            val loadbalancerServers = fetchLoadbalancerServers(loadbalancer.id)

            val pendingLoadbalancerActions =
                api.loadBalancers.actions(loadbalancer.id).actions.filter { it.status == ActionStatus.RUNNING }
            if (pendingLoadbalancerActions.isNotEmpty()) {
                logInfo("load balancer '${loadbalancer.name}' has pending actions")
                pendingLoadbalancerActions.forEach {
                    api.loadBalancers.waitForAction(it)
                }
            }

            val allServersForCurrentDeployment =
                api.servers.list(
                    labelSelectors = mapOf(
                        managedByLabel to LabelSelectorValue.Equals("asg"),
                        deploymentIdLabel to LabelSelectorValue.Equals(deploymentId)
                    )
                )

            for (server in allServersForCurrentDeployment) {
                val actions = api.servers.actions(server.id).actions.filter { it.status == ActionStatus.RUNNING }
                if (actions.isNotEmpty()) {
                    logInfo("server '${server.name}' has pending actions")
                    actions.forEach {
                        api.servers.waitForAction(it)
                    }
                }
            }

            val allCurrentlyNotAttachedServers =
                allServersForCurrentDeployment.filter { potentiallyNotAttached -> loadbalancerServers.none { it.id == potentiallyNotAttached.id } }

            if (allCurrentlyNotAttachedServers.isNotEmpty()) {

                for (server in allCurrentlyNotAttachedServers) {
                    logInfo("attaching server '${server.name}' to load balancer '${loadbalancer.name}'")

                    try {
                        val action = api.loadBalancers.attachServer(loadbalancer.id, server.id)
                        if (!api.loadBalancers.waitForAction(action)) {
                            logError("attaching server '${server.name}' to load balancer '${loadbalancer.name}' failed")
                            continue
                        }
                    } catch (e: HetznerApiException) {
                        if (e.error.code != HetznerApiErrorType.TARGET_ALREADY_DEFINED) {
                            logError("attaching server '$server.name' to load balancer '${loadbalancer.name}' failed")
                            continue
                        }
                    }
                }

                continue
            }

            val upToDateServers = loadbalancerServers.filter {
                it.isUpToDate(userDataHash)
            }

            val serversToRotate = loadbalancerServers.filter {
                !it.isUpToDate(userDataHash)
            }

            logInfo("found ${loadbalancerServers.size} server(s) in total, ${serversToRotate.size} outdated and ${upToDateServers.size} up to date")

            val updatedButNoYetHealthyServers = upToDateServers.filter {
                it.age < healthTimeout && it.status != LoadBalancerHealthStatus.healthy
            }
            if (updatedButNoYetHealthyServers.isNotEmpty()) {
                logInfo("waiting for ${updatedButNoYetHealthyServers.statusLogText()} to become healthy within ${healthTimeout}")
                continue
            }

            val updatedButFailedServers = upToDateServers.filter {
                it.age > healthTimeout && it.status != LoadBalancerHealthStatus.healthy
            }
            if (updatedButFailedServers.isNotEmpty()) {
                logInfo("servers ${updatedButFailedServers.statusLogText()} did not become healthy within ${healthTimeout}, deleting...")
                for (server in updatedButFailedServers) {
                    logInfo("deleting server '${server.name}'")
                    try {
                        val delete = api.servers.delete(server.id)
                        if (!api.servers.waitForAction(delete)) {
                            logError("deleting server '${server.name}' failed")
                            continue
                        }
                    } catch (e: HetznerApiException) {
                        logError("deleting server '${server.name}' failed")
                        continue
                    }
                }
            }

            val serverName = "${serverNamePrefix}-${upToDateServers.size}"
            if (upToDateServers.size != replicas) {
                var server = api.servers.get(serverName)
                if (server == null) {
                    logInfo("creating server '$serverName'")
                    try {
                        val labels = HetznerLabels()
                        labels.addLabel(managedByLabel, "asg")
                        labels.addLabel(loadBalancerIdLabel, loadbalancer.id.toString())
                        labels.addLabel(deploymentIdLabel, deploymentId)
                        labels.addLabel(userDataHashLabel, userDataHash)

                        server = api.servers.create(
                            ServerCreateRequest(
                                serverName,
                                locationRef.reference,
                                serverTypeRef.reference,
                                placementGroupRef?.reference,
                                imageRef.reference,
                                sshKeyRefs.map { it.reference },
                                networks.map { it.network.id.toString() },
                                firewallRefs.map { it.reference },
                                userData,
                                labels.rawLabels(),
                                PublicNet(enableIpv4, enableIpv6)
                            )
                        )

                        if (server == null || server.action == null) {
                            logError("creation failed for server '$serverName'")
                            continue
                        }

                        val result = api.waitForAction(server.action.id, {
                            api.servers.action(it)
                        })

                        if (!result) {
                            logError("creation failed for server '$serverName'")
                            continue
                        }

                    } catch (e: HetznerApiException) {
                        logError("creation failed for server '$serverName'")
                        continue
                    }
                }
            }

            val updatedAndHealthyServers = upToDateServers.filter {
                it.status == LoadBalancerHealthStatus.healthy
            }
            finished = updatedAndHealthyServers.count() == replicas
        }

        api.servers.list(labelSelectors = mapOf(loadBalancerIdLabel to LabelSelectorValue.Equals(loadbalancer.id.toString())))
            .filter {
                it.labels[deploymentIdLabel] != deploymentId
            }.forEach {
            logInfo("deleting outdated server '${it.name}'")
            val delete = api.servers.delete(it.id)
            api.servers.waitForAction(delete)
        }

        ASG_ROTATE_STATUS.OK
    }

    fun List<LoadbalancerServer>.updatedCount(userDataHash: String) = this.count {
        it.isUpToDate(userDataHash)
    }
}
