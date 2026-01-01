@file:Suppress("ktlint:standard:enum-entry-name-case")

package de.solidblocks.cli.hetzner.asg

import com.github.ajalt.clikt.core.CliktError
import de.solidblocks.cli.hetzner.Constants
import de.solidblocks.cli.hetzner.Constants.DEPLOYMENT_ID_LABEL
import de.solidblocks.cli.hetzner.Constants.LOADBALANCER_ID_LABEL
import de.solidblocks.cli.hetzner.Constants.MANAGED_BY_LABEL
import de.solidblocks.cli.hetzner.Constants.USER_DATA_HASH_LABEL
import de.solidblocks.cli.hetzner.HetznerLabels
import de.solidblocks.cli.hetzner.hashString
import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.HetznerApiErrorType
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.resources.*
import de.solidblocks.hetzner.cloud.resources.LoadBalancerTargetType.ip
import de.solidblocks.hetzner.cloud.resources.LoadBalancerTargetType.label_selector
import de.solidblocks.utils.logErrorBlcks
import de.solidblocks.utils.logInfoBlcks
import de.solidblocks.utils.logWarningBlcks
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class ServerInfoAttachmentType {
    direct,
    label_selector,
}

data class LoadbalancerServer
@OptIn(ExperimentalTime::class)
constructor(
    val name: String,
    val id: Long,
    private val lbStatus:
    List<de.solidblocks.hetzner.cloud.resources.LoadBalancerHealthStatusResponse>,
    val attachment: ServerInfoAttachmentType,
    val created: Instant,
    val labels: Map<String, String>,
    val selector: String? = null,
) {
    fun isUpToDate(userDataHash: String) =
        HetznerLabels(labels).labelMatches(USER_DATA_HASH_LABEL, userDataHash)

    @OptIn(ExperimentalTime::class)
    val age: Duration
        get() = Clock.System.now().minus(created)

    val isManaged: Boolean
        get() = labels.containsKey(Constants.MANAGED_BY_LABEL)

    fun status(coolDownTime: Duration) =
        if (age < coolDownTime) {
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

fun List<LoadbalancerServer>.statusLogText() =
    this.joinToString(", ") { "'${it.name}' (age: ${it.age})" }

fun List<LoadbalancerServer>.logText() = this.joinToString(", ") { "'${it.name}'" }

enum class AsgRotateStatus {
    OK,
    TIMEOUT,
    LOADBALANCER_NOT_FOUND,
}

class HetznerAsg(hcloudToken: String) {

    val api = HetznerApi(hcloudToken)

    @OptIn(ExperimentalTime::class)
    suspend fun fetchLoadbalancerServers(id: Long): List<LoadbalancerServer> {
        val loadbalancer =
            api.loadBalancers.get(id) ?: throw RuntimeException("loadbalancer '$id' not found")

        val list = mutableListOf<LoadbalancerServer>()

        loadbalancer.targets.forEach { lbTarget ->
            if (lbTarget.type == LoadBalancerTargetType.server && lbTarget.server != null) {
                val server = api.servers.get(lbTarget.server!!.id)
                if (server != null) {
                    list.add(
                        LoadbalancerServer(
                            server.name,
                            server.id,
                            lbTarget.status,
                            ServerInfoAttachmentType.direct,
                            server.created,
                            labels = server.labels,
                        ),
                    )
                }
            }

            if (lbTarget.type == label_selector &&
                lbTarget.labelSelector != null &&
                lbTarget.targets != null
            ) {
                lbTarget.targets!!.forEach {
                    val server = api.servers.get(it.server.id)
                    if (server != null) {
                        list.add(
                            LoadbalancerServer(
                                server.name,
                                server.id,
                                it.status,
                                ServerInfoAttachmentType.label_selector,
                                server.created,
                                labels = server.labels,
                                lbTarget.labelSelector!!.selector,
                            ),
                        )
                    }
                }
            }
        }

        return list
    }

    suspend fun ensureLoadBalancer(loadbalancer: LoadBalancerReference): LoadBalancerResponse? {
        val lbByName = api.loadBalancers.get(loadbalancer.reference)
        val lbById =
            try {
                val id = loadbalancer.reference.toLong()
                api.loadBalancers.get(id)
            } catch (e: Exception) {
                null
            }

        if (lbById == null && lbByName == null) {
            logErrorBlcks("load balancer '${loadbalancer.reference}' not found.")
        }

        return lbByName ?: lbById
    }

    @OptIn(ExperimentalTime::class)
    fun rotate(
        loadbalancerRef: LoadBalancerReference,
        locationRef: LocationReference,
        serverTypeRef: ServerTypeReference,
        imageRef: ImageReference,
        userData: String,
        replicas: Int = 1,
        namePrefix: String? = null,
        sshKeyRefs: List<SSHKeyReference> = emptyList(),
        firewallRefs: List<FirewallReference> = emptyList(),
        networkRefs: List<NetworkReference> = emptyList(),
        placementGroupRef: PlacementGroupReference? = null,
        enableIpv4: Boolean = true,
        enableIpv6: Boolean = false,
        healthTimeout: Duration = 5.minutes,
        totalTimeout: Duration = 10.minutes,
    ): AsgRotateStatus = runBlocking {
        val deploymentStart = Clock.System.now()
        val loadbalancer =
            ensureLoadBalancer(loadbalancerRef)
                ?: return@runBlocking AsgRotateStatus.LOADBALANCER_NOT_FOUND

        val networks =
            if (loadbalancer.privateNetworks.isNotEmpty()) {
                val networks = loadbalancer.privateNetworks.mapNotNull { api.networks.get(it.network) }
                logInfoBlcks(
                    "load balancer '${loadbalancer.name}' is attached to private network(s) ${
                        networks.joinToString(
                            ", ",
                        ) { "'${it.name}'" }
                    }, new servers will be attached to those networks as well",
                )
                networks
            } else {
                logInfoBlcks(
                    "load balancer '${loadbalancer.name}' has no private network, will use public ip for new servers",
                )
                emptyList()
            }

        if (loadbalancer.targets.any { it.type == ip }) {
            throw CliktError("ip based targets are not supported for server rotation")
        }

        val userDataHash = hashString(userData)

        logInfoBlcks("hash for provided user data is '$userDataHash'")
        logInfoBlcks("inspecting load balancer '${loadbalancer.name}'")

        val coolDownTime =
            loadbalancer.services.maxOf { it.healthCheck.retries * it.healthCheck.interval }.seconds +
                    10.seconds
        logInfoBlcks("using '$coolDownTime' as cool down time for newly created servers")
        val servers = fetchLoadbalancerServers(loadbalancer.id)

        servers
            .filter { it.attachment == ServerInfoAttachmentType.direct }
            .let {
                if (it.isNotEmpty()) {
                    logInfoBlcks(
                        "found ${it.size} attached server(s) (${it.joinToString(", ") { "'${it.name}'" }}) on load balancer '${loadbalancer.name}'",
                    )
                }
            }

        servers
            .filter { it.attachment == ServerInfoAttachmentType.label_selector }
            .let {
                if (it.isNotEmpty()) {
                    logInfoBlcks(
                        "found ${it.size} servers (${it.joinToString(", ") { "'${it.name}'" }}) on load balancer '${loadbalancer.name}' matching label selector '${it.first().selector ?: "<unknown>"}'",
                    )
                }
            }

        if (servers.isEmpty()) {
            logInfoBlcks("no servers currently attached to load balancer '${loadbalancer.name}'")
        } else {
            logInfoBlcks("inspecting servers for  load balancer '${loadbalancer.name}'")
        }

        var finished = false
        val deploymentId =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(java.time.Instant.now())

        val serverNamePrefix = "${namePrefix ?: loadbalancer.name}-$deploymentId"
        val controlLoopDelay = 5.seconds

        logInfoBlcks(
            "starting rollout '$deploymentId' to $replicas replicas, server name prefix is '$serverNamePrefix' and timeout for newly created servers is '$healthTimeout'",
        )

        while (!finished) {
            if (Clock.System.now().minus(deploymentStart) > totalTimeout) {
                logErrorBlcks("rotation timeout of $totalTimeout reached, aborting")
                return@runBlocking AsgRotateStatus.TIMEOUT
            }

            sleep(controlLoopDelay.inWholeMilliseconds)

            val loadbalancerServers = fetchLoadbalancerServers(loadbalancer.id)

            val pendingLoadbalancerActions =
                api.loadBalancers.actions(loadbalancer.id).actions.filter {
                    it.status == ActionStatus.RUNNING
                }

            if (pendingLoadbalancerActions.isNotEmpty()) {
                logInfoBlcks("load balancer '${loadbalancer.name}' has pending actions")
                pendingLoadbalancerActions.forEach { api.loadBalancers.waitForAction(it) { logInfoBlcks(it) } }
            }

            val allServersForCurrentDeployment =
                api.servers.list(
                    labelSelectors =
                        mapOf(
                            MANAGED_BY_LABEL to LabelSelectorValue.Equals("asg"),
                            DEPLOYMENT_ID_LABEL to LabelSelectorValue.Equals(deploymentId),
                        ),
                )

            for (server in allServersForCurrentDeployment) {
                val actions =
                    api.servers.actions(server.id).actions.filter { it.status == ActionStatus.RUNNING }
                if (actions.isNotEmpty()) {
                    logInfoBlcks("server '${server.name}' has pending actions")
                    actions.forEach { api.servers.waitForAction(it) { logInfoBlcks(it) } }
                }
            }

            val allCurrentlyNotAttachedServers =
                allServersForCurrentDeployment.filter { potentiallyNotAttached ->
                    loadbalancerServers.none { it.id == potentiallyNotAttached.id }
                }

            if (allCurrentlyNotAttachedServers.isNotEmpty()) {
                for (server in allCurrentlyNotAttachedServers) {
                    logInfoBlcks("attaching server '${server.name}' to load balancer '${loadbalancer.name}'")

                    try {
                        val action = api.loadBalancers.attachServer(loadbalancer.id, server.id)
                        if (!api.loadBalancers.waitForAction(action, { logInfoBlcks(it) })) {
                            logErrorBlcks(
                                "attaching server '${server.name}' to load balancer '${loadbalancer.name}' failed",
                            )
                            continue
                        }
                    } catch (e: HetznerApiException) {
                        if (e.error.code != HetznerApiErrorType.TARGET_ALREADY_DEFINED) {
                            logErrorBlcks(
                                "attaching server '$server.name' to load balancer '${loadbalancer.name}' failed",
                            )
                            continue
                        }
                    }
                }

                continue
            }

            val unmanagedServers = loadbalancerServers.filter { !it.isManaged }

            val managedServers = loadbalancerServers.filter { it.isManaged }

            val upToDateServers = managedServers.filter { it.isUpToDate(userDataHash) }

            val serversToRotate = managedServers.filter { !it.isUpToDate(userDataHash) }

            if (unmanagedServers.isNotEmpty()) {
                logWarningBlcks(
                    "ignoring ${unmanagedServers.size} unmanaged server(s) (${unmanagedServers.logText()})",
                )
            }
            logInfoBlcks(
                "rotating ${managedServers.size} managed server(s), ${serversToRotate.size} outdated and ${upToDateServers.size} up to date",
            )

            val updatedButNoYetHealthyServers =
                upToDateServers.filter {
                    it.age < healthTimeout && it.status(coolDownTime) != LoadBalancerHealthStatus.healthy
                }
            if (updatedButNoYetHealthyServers.isNotEmpty()) {
                logInfoBlcks(
                    "waiting for servers ${updatedButNoYetHealthyServers.statusLogText()} to become healthy within $healthTimeout",
                )
                continue
            }

            val updatedButFailedServers =
                upToDateServers.filter {
                    it.age > healthTimeout && it.status(coolDownTime) != LoadBalancerHealthStatus.healthy
                }
            if (updatedButFailedServers.isNotEmpty()) {
                logInfoBlcks(
                    "servers ${updatedButFailedServers.statusLogText()} did not become healthy within $healthTimeout, deleting...",
                )
                for (server in updatedButFailedServers) {
                    logInfoBlcks("deleting server '${server.name}'")
                    try {
                        val delete = api.servers.delete(server.id)
                        if (!api.servers.waitForAction(delete, { logInfoBlcks(it) })) {
                            logErrorBlcks("deleting server '${server.name}' failed")
                            continue
                        }
                    } catch (e: HetznerApiException) {
                        logErrorBlcks("deleting server '${server.name}' failed")
                        continue
                    }
                }
            }

            val serverName = "$serverNamePrefix-${upToDateServers.size}"
            if (upToDateServers.size != replicas) {
                if (api.servers.get(serverName) == null) {
                    logInfoBlcks("creating server '$serverName'")
                    try {
                        val labels = HetznerLabels()
                        labels.addLabel(MANAGED_BY_LABEL, "asg")
                        labels.addLabel(LOADBALANCER_ID_LABEL, loadbalancer.id.toString())
                        labels.addLabel(DEPLOYMENT_ID_LABEL, deploymentId)
                        labels.addLabel(USER_DATA_HASH_LABEL, userDataHash)

                        val newServer =
                            api.servers.create(
                                ServerCreateRequest(
                                    serverName,
                                    locationRef.reference,
                                    serverTypeRef.reference,
                                    imageRef.reference,
                                    placementGroupRef?.reference,
                                    sshKeyRefs.map { it.reference.toLong() },
                                    networks.map { it.id.toString() } + networkRefs.map { it.reference },
                                    firewallRefs.map { it.reference },
                                    emptyList(),
                                    userData,
                                    labels.rawLabels(),
                                    PublicNet(enableIpv4, enableIpv6),
                                ),
                            )

                        if (newServer == null || newServer.action == null) {
                            logErrorBlcks("error while creating server '$serverName'")
                            continue
                        }

                        val result =
                            api.waitForAction(newServer.action.id, { logInfoBlcks(it) }, { api.servers.action(it) })

                        if (!result) {
                            logErrorBlcks("server '$serverName' did not become ready within timeout")
                            continue
                        }
                    } catch (e: HetznerApiException) {
                        logErrorBlcks(
                            "creation failed for server '$serverName', error was '${e.error.message}' (${e.error.code}), details: ${
                                e.error.details?.let {
                                    it.fields?.joinToString {
                                        "field: ${it.name}, error: ${
                                            (it.messages ?: emptyList()).joinToString(
                                                ", ",
                                            )
                                        }"
                                    }
                                } ?: "<none>"
                            }",
                        )
                        continue
                    }
                }
            }

            val updatedAndHealthyServers =
                upToDateServers.filter { it.status(coolDownTime) == LoadBalancerHealthStatus.healthy }
            if (updatedAndHealthyServers.isNotEmpty()) {
                logInfoBlcks(
                    "wanted $replicas healthy servers found ${updatedAndHealthyServers.size} (${updatedAndHealthyServers.statusLogText()})",
                )
            }

            finished = updatedAndHealthyServers.count() >= replicas
        }

        val loadbalancerServers = fetchLoadbalancerServers(loadbalancer.id)

        val updatedAndHealthyServers =
            loadbalancerServers
                .filter {
                    it.status(coolDownTime) == LoadBalancerHealthStatus.healthy &&
                            it.isUpToDate(userDataHash)
                }
                .sortedBy { it.age }

        if (updatedAndHealthyServers.count() > replicas) {
            logInfoBlcks(
                "wanted $replicas and have ${updatedAndHealthyServers.count()} removing ${updatedAndHealthyServers.count() - replicas}",
            )

            updatedAndHealthyServers.take(updatedAndHealthyServers.count() - replicas).forEach {
                logInfoBlcks("deleting server '${it.name}'")
                val delete = api.servers.delete(it.id)
                api.servers.waitForAction(delete, { logInfoBlcks(it) })
            }
        }

        val outdatedServers =
            loadbalancerServers
                .filter { it.isManaged && !it.isUpToDate(userDataHash) }
                .distinctBy { it.id }

        if (outdatedServers.isNotEmpty()) {
            logInfoBlcks(
                "deleting outdated managed servers: ${outdatedServers.joinToString(", ") { "'${it.name}'" }}",
            )
            outdatedServers.forEach {
                logInfoBlcks("deleting outdated managed server '${it.name}'")
                val delete = api.servers.delete(it.id)
                api.servers.waitForAction(delete) { logInfoBlcks(it) }
            }
        }

        AsgRotateStatus.OK
    }
}
