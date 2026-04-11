package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.Constants.sshKeysLabel
import de.solidblocks.cloud.Constants.userDataLabel
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.equalsIgnoreOrder
import de.solidblocks.cloud.joinToStringOrEmpty
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeLookup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.HetznerLabels
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerApiErrorType
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.hetzner.cloud.resources.ServerNetworkAttachRequest
import de.solidblocks.hetzner.cloud.resources.ServerUpdateRequest
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class HetznerServerProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerServerLookup, HetznerServerRuntime>,
    InfrastructureResourceProvisioner<HetznerServer, HetznerServerRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: HetznerServerLookup, context: CloudProvisionerContext) = api.servers.get(lookup.name)?.let {
        HetznerServerRuntime(
            it.id,
            it.name,
            it.status,
            it.image.name ?: "unknown",
            HetznerServerType.valueOf(it.type.name),
            HetznerLocation.valueOf(it.location.name),
            it.labels,
            it.volumes
                .mapNotNull { api.volumes.get(it) }
                .mapNotNull { context.lookup(HetznerVolumeLookup(it.name)) },
            it.privateNetwork.firstOrNull()?.ip,
            it.publicNetwork?.ipv4?.ip,
            it.publicNetwork?.ipv4?.ip?.let { listOf(Endpoint(it, 22, EndpointProtocol.ssh)) }
                ?: emptyList(),
        )
    }

    override suspend fun apply(resource: HetznerServer, context: CloudProvisionerContext, log: LogContext): Result<HetznerServerRuntime> {
        var server = lookup(resource.asLookup(), context)

        val sshKeys =
            resource.sshKeys.map {
                context.lookup(it)
                    ?: return Error<HetznerServerRuntime>("failed to lookup ${it.logText()}")
            }

        val volumes =
            resource.volumes.map {
                val volume =
                    context.lookup(it)
                        ?: return Error<HetznerServerRuntime>("failed to resolve ${it.logText()}")

                /*
                if (volume.server != null) {
                    return Error<HetznerServerRuntime>(
                        "${it.logText()} already attached to server ${volume.server}",
                    )
                }*/

                volume
            }

        val userData = context.ensureLookup(resource.userData)

        val labels = HetznerLabels().let {
            it.addHashedLabel(sshKeysLabel, sshKeys.joinToString { it.fingerprint })
            it.addHashedLabel(userDataLabel, userData.userData)
            it.rawLabels()
        } + resource.labels

        if (server == null) {
            logger.info { "server '${resource.name}' not found, creating" }

            logDebug(
                "using ssh key(s): ${
                    sshKeys.let {
                        if (it.isEmpty()) {
                            "<none>"
                        } else {
                            it.joinToString(", ") { "${it.name} (${it.id})" }
                        }
                    }
                }",
                context = log,
            )
            logDebug(
                "using volume(s): ${
                    volumes.let {
                        if (it.isEmpty()) {
                            "<none>"
                        } else {
                            it.joinToString(", ") { "${it.name} (${it.id})" }
                        }
                    }
                }",
                context = log,
            )
            logger.info {
                "creating server '${resource.name}' with ssh keys ${sshKeys.joinToString(", ") { "${it.name} (${it.id})" }}"
            }
            val request =
                ServerCreateRequest(
                    resource.name,
                    resource.location,
                    resource.type,
                    image = resource.image,
                    userData = userData.userData,
                    sshKeys = sshKeys.map { it.id },
                    volumes = volumes.map { it.id },
                    labels = labels,
                )
            val createRequest = api.servers.create(request)

            if (
                !api.servers.waitForAction(createRequest.action) {
                    log.info("waiting for creation of ${resource.logText()}")
                }
            ) {
                return Error(
                    "failed to wait for creation of '${resource.name}', you may need to remove the resource manually and retry",
                )
            }

            server = lookup(resource.asLookup(), context)
        }

        if (server == null) {
            return Error("server creation failed")
        }

        api.servers.update(server.id, ServerUpdateRequest(resource.name, labels))

        if (resource.subnet != null) {
            val subnet =
                context.lookup(resource.subnet)
                    ?: return Error("subnet '${resource.subnet.name}' not found")

            if (resource.privateIp != null) {
                try {
                    val action =
                        api.servers.attachToNetwork(
                            server.id,
                            ServerNetworkAttachRequest(subnet.network, resource.privateIp),
                        )
                    api.networks.waitForAction(action) {
                        log.info("waiting for attachment to ${subnet.logText()}")
                    }
                } catch (e: HetznerApiException) {
                    if (e.error.code != HetznerApiErrorType.SERVER_ALREADY_ATTACHED) {
                        return Error("hetzner api error '${e.error.code}'")
                    }
                }
            }
        }

        return lookup(resource.asLookup(), context)?.let {
            log.debug("${resource.logText()} has public ip ${it.publicIpv4 ?: "<none>"}")
            Success(it)
        } ?: Error<HetznerServerRuntime>("error creating ${resource.logText()}")
    }

    override suspend fun diff(resource: HetznerServer, context: CloudProvisionerContext): ResourceDiff? {
        val runtime = lookup(resource.asLookup(), context)

        return if (runtime != null) {
            val changes = mutableListOf<ResourceDiffItem>()

            val labels = HetznerLabels(runtime.labels)

            val sshKeys =
                resource.sshKeys.map {
                    context.lookup(it) ?: throw RuntimeException("failed to lookup ${it.logText()}")
                }

            val sshKeysHash =
                labels.hashLabelMatches(sshKeysLabel, sshKeys.joinToString { it.fingerprint })

            changes.addAll(createLabelDiff(resource, runtime))

            if (!sshKeysHash.matches) {
                changes.add(
                    ResourceDiffItem(
                        "ssh keys",
                        triggersRecreate = true,
                        changed = true,
                        expectedValue = sshKeysHash.expectedValue,
                        actualValue = sshKeysHash.actualValue,
                    ),
                )
            }

            if (resource.location != runtime.location) {
                changes.add(
                    ResourceDiffItem(
                        "location",
                        triggersRecreate = true,
                        changed = true,
                        expectedValue = resource.location,
                        actualValue = runtime.location,
                    ),
                )
            }

            if (resource.privateIp != null && resource.privateIp != runtime.privateIpv4) {
                changes.add(
                    ResourceDiffItem(
                        "private ip address",
                        triggersRecreate = false,
                        changed = true,
                        expectedValue = resource.privateIp,
                        actualValue = runtime.privateIpv4,
                    ),
                )
            }

            val userData =
                context.lookup(resource.userData)
                    ?: return ResourceDiff(
                        resource,
                        has_changes,
                        changes =
                        listOf(
                            ResourceDiffItem(
                                "user data checksum",
                                triggersRecreate = true,
                                changed = true,
                            ),
                        ),
                    )
            val userDataHash =
                labels.hashLabelMatches(
                    userDataLabel,
                    userData.userData,
                )

            if (!userDataHash.matches) {
                changes.add(
                    ResourceDiffItem(
                        "user data checksum",
                        triggersRecreate = true,
                        changed = true,
                        expectedValue = userDataHash.expectedValue,
                        actualValue = userDataHash.actualValue,
                    ),
                )
            }

            if (resource.type != runtime.type) {
                changes.add(ResourceDiffItem("type", true, true, false, resource.type, runtime.type))
            }

            if (!(resource.volumes.map { it.name } equalsIgnoreOrder runtime.volumes.map { it.name })) {
                changes.add(
                    ResourceDiffItem(
                        "volumes",
                        true,
                        true,
                        false,
                        resource.volumes.joinToStringOrEmpty { it.name },
                        runtime.volumes.joinToStringOrEmpty { it.name },
                    ),
                )
            }

            if (resource.image != runtime.image) {
                changes.add(ResourceDiffItem("image", true, true, false, resource.image, runtime.image))
            }

            if (changes.isEmpty()) {
                ResourceDiff(resource, up_to_date)
            } else {
                ResourceDiff(resource, has_changes, changes = changes)
            }
        } else {
            ResourceDiff(resource, missing)
        }
    }

    override suspend fun destroy(resource: HetznerServer, context: CloudProvisionerContext, logContext: LogContext) = lookup(resource.asLookup(), context)?.let {
        val delete = api.servers.delete(it.id)
        api.servers.waitForAction(delete) {
            logContext.info("waiting for deletion of ${resource.logText()}")
        }
    } ?: false

    override val supportedLookupType: KClass<*> = HetznerServerLookup::class

    override val supportedResourceType: KClass<*> = HetznerServer::class
}
