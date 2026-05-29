package de.solidblocks.cloud.services.s3

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.defaultServiceSubnet
import de.solidblocks.cloud.Constants.dnsRecordLabels
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.serverPrivateIp
import de.solidblocks.cloud.Constants.serviceLabels
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.types.backup.createBackupConfiguration
import de.solidblocks.cloud.providers.types.backup.createBackupResources
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.context.ValidationContext
import de.solidblocks.cloud.provisioner.context.ensureLookup
import de.solidblocks.cloud.provisioner.context.ensureOptionalLookup
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKey
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucket
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayout
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermission
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.RandomSecret
import de.solidblocks.cloud.provisioner.secret.StaticSecret
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.toResult
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketAccessKeyConfigurationRuntime
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketConfigurationRuntime
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationRuntime
import de.solidblocks.cloud.status.serverStatusMarkdown
import de.solidblocks.cloud.status.withServerStatus
import de.solidblocks.cloud.utils.*
import de.solidblocks.cloudinit.GarageFsUserData
import de.solidblocks.cloudinit.GarageFsUserData.Companion.s3AdminHost
import de.solidblocks.cloudinit.GarageFsUserData.Companion.s3Host
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logError
import kotlinx.coroutines.runBlocking

class S3ServiceManager : ServiceManager<S3ServiceConfiguration, S3ServiceConfigurationRuntime> {

    private fun serviceRootDomain(cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime) = "${serverName(cloud.environmentContext, runtime.name, 0)}.${cloud.rootDomain}"

    override fun maintenance(cloud: CloudConfigurationRuntime, runtime: S3ServiceConfigurationRuntime, context: SSHProvisionerContext, log: LogContext): Result<Unit> =
        serverMaintenance(cloud, runtime, context, log)

    override fun status(cloud: CloudConfigurationRuntime, runtime: S3ServiceConfigurationRuntime, context: SSHProvisionerContext): Result<String> {
        val serverName = serverName(cloud.environmentContext, runtime.name, 0)

        val result = context.withServerStatus(serverName) { sshClient, status ->
            status
        }

        return markdown {
            h1("Service ${runtime.name}")
            h2("Server $serverName")
            serverStatusMarkdown(result)
        }.let { Success(it) }
    }

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: S3ServiceConfigurationRuntime, context: ProvisionerContext): Result<List<BaseInfrastructureResource<*>>> {
        val serverName = serverName(cloud.environmentContext, runtime.name, 0)

        val defaultResources = this.createDefaultServerResources(cloud, runtime, 0)

        val backupResources = createBackupResources(cloud.backupProviderRuntime(), cloud, serverName, runtime, context.environment)

        val adminToken = GenericSecret<GenericSecretRuntime>(
            secretPath(cloud.environmentContext, runtime, listOf("garage", "admin_token")),
            RandomSecret(
                length = 64,
                allowedChars = ('a'..'f') + ('0'..'9'),
            ),
            true,
        )

        val rpcSecret = GenericSecret<GenericSecretRuntime>(
            secretPath(cloud.environmentContext, runtime, listOf("garage", "rpc_secret")),
            RandomSecret(
                length = 64,
                allowedChars = ('a'..'f') + ('0'..'9'),
            ),
            true,
        )

        val metricsToken = GenericSecret<GenericSecretRuntime>(
            secretPath(cloud.environmentContext, runtime, listOf("garage", "metrics_token")),
            RandomSecret(
                length = 64,
                allowedChars = ('a'..'f') + ('0'..'9'),
            ),
            true,
        )

        val configurationEnvironmentVars = (cloud.environmentVars + runtime.environmentVars).map {
            val resolvedValue = when (val result = context.interpolationRegistry().resolve(it.value)) {
                is Error<String> -> return Error(result.error)
                is Success<String> -> result.data
            }

            it.key to resolvedValue
        }.toMap()

        val userData = UserData(
            setOf(defaultResources.volumes.data, adminToken, rpcSecret, metricsToken) + backupResources.first,
            { context ->
                if (listOf(adminToken, rpcSecret, metricsToken).any {
                        context.lookup(it.asLookup()) == null
                    }
                ) {
                    return@UserData null
                }

                GarageFsUserData(
                    runtime.name,
                    context.ensureLookup(defaultResources.volumes.data.asLookup()).device,
                    createBackupConfiguration(cloud.backupProviderRuntime(), cloud, runtime, context, backupResources.second),
                    serviceRootDomain(cloud, runtime),
                    configurationEnvironmentVars,
                    context.ensureLookup(rpcSecret.asLookup()).secret,
                    context.ensureLookup(adminToken.asLookup()).secret,
                    context.ensureLookup(metricsToken.asLookup()).secret,
                    runtime.buckets.map {
                        de.solidblocks.cloudinit.GarageFsBucket(
                            it.name,
                            it.managedPublicWebAccessDomains.values.toSet() + it.manuallyManagedPublicWebAccessDomains,
                        )
                    },
                    true,
                    context.ensureOptionalLookup(defaultResources.floatingIp?.asLookup())?.ip,
                ).toResult(context, defaultResources.sshIdentity)
            },
        )

        val firewall = runtime.firewall(cloud, listOf(80, 443))

        val server = HetznerServer(
            serverName,
            userData = userData,
            location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud.environmentContext))),
            volumes = setOf(defaultResources.volumes.data.asLookup()) + setOfNotNull(backupResources.second?.asLookup()),
            type = cloud.hetznerProviderRuntime().defaultInstanceType,
            subnet = HetznerSubnetLookup(
                defaultServiceSubnet,
                HetznerNetworkLookup(networkName(cloud.environmentContext)),
            ),
            privateIp = serverPrivateIp(runtime.index),
            labels = serviceLabels(runtime) + cloudLabels(cloud.environmentContext),
            dependsOn = backupResources.first + defaultResources.list(),
            floatingIp = defaultResources.floatingIp?.asLookup(),
        )

        if (cloud.rootDomain == null) {
            throw IllegalArgumentException("root domain is required")
        }

        val zone = HetznerDnsZoneLookup(cloud.rootDomain)
        val serverDnsRecord = createDefaultServerDnsRecord(cloud, runtime, server.asLookup(), defaultResources.floatingIp?.asLookup())!!

        val catchAllDomain = HetznerDnsRecord(
            "*.${serverName(cloud.environmentContext, runtime.name, 0)}",
            zone,
            listOf(server.asLookup()),
            emptyList(),
            labels =
            dnsRecordLabels(runtime) + cloudLabels(cloud.environmentContext),
        )
        val dnsResources = runtime.buckets.flatMap { it.managedPublicWebAccessDomains.entries }.map {
            if (it.key.isEmpty()) {
                HetznerDnsRecord(
                    "@",
                    HetznerDnsZoneLookup(it.value),
                    listOf(server.asLookup()),
                    emptyList(),
                    labels = dnsRecordLabels(runtime) + cloudLabels(cloud.environmentContext),
                )
            } else {
                HetznerDnsRecord(
                    it.value,
                    HetznerDnsZoneLookup(it.value),
                    listOf(server.asLookup()),
                    emptyList(),
                    labels = dnsRecordLabels(runtime) + cloudLabels(cloud.environmentContext),
                )
            }
        } + listOf(serverDnsRecord, catchAllDomain)

        val layout = GarageFsLayout(
            ByteSize.fromGigabytes(runtime.instance.volumeSize).bytes,
            server.asLookup(),
            adminToken.asLookup(),
        )
        val bucketResources = mutableListOf<BaseInfrastructureResource<*>>()

        runtime.buckets.forEach {
            val bucket = GarageFsBucket(
                it.name,
                server.asLookup(),
                adminToken.asLookup(),
                websiteAccess = it.publicAccess,
                emptyList(),
                setOf(layout),
            )
            bucketResources.add(bucket)

            it.accessKeys.forEach { accessKeyRuntime ->
                val accessKey = GarageFsAccessKey(
                    secretPath(cloud.environmentContext, runtime, listOf(accessKeyRuntime.name)),
                    server,
                    adminToken,
                    setOf(bucket),
                )
                bucketResources.add(accessKey)

                bucketResources.add(
                    GenericSecret<GenericSecretRuntime>(
                        secretPath(cloud.environmentContext, runtime, listOf("buckets", bucket.name, accessKeyRuntime.name, "secret_key")),
                        StaticSecret { it.ensureLookup(accessKey.asLookup()).secretAccessKey },
                        true,
                        dependsOn = setOf(accessKey.asLookup()),
                    ),
                )

                bucketResources.add(
                    GenericSecret<GenericSecretRuntime>(
                        secretPath(cloud.environmentContext, runtime, listOf("buckets", bucket.name, accessKeyRuntime.name, "access_key")),
                        StaticSecret { it.ensureLookup(accessKey.asLookup()).id },
                        true,
                        dependsOn = setOf(accessKey.asLookup()),
                    ),
                )

                val permission = GarageFsPermission(
                    bucket,
                    accessKey,
                    server,
                    adminToken,
                    accessKeyRuntime.owner,
                    accessKeyRuntime.read,
                    accessKeyRuntime.write,
                    setOf(bucket),
                )
                bucketResources.add(permission)
            }
        }

        val s3HostSecret = GenericSecret<GenericSecretRuntime>(
            secretPath(cloud.environmentContext, runtime, listOf("endpoints", "s3_host")),
            StaticSecret { s3Host(serviceRootDomain(cloud, runtime)) },
            true,
        )

        return Success(listOf(s3HostSecret, firewall, server, adminToken, rpcSecret, metricsToken, layout) + bucketResources + dnsResources + defaultResources.list())
    }

    override fun createProvisioners(runtime: S3ServiceConfigurationRuntime) = listOf<InfrastructureResourceProvisioner<*, *, *>>()

    override fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: S3ServiceConfiguration,
        context: ValidationContext,
        log: LogContext,
    ): Result<S3ServiceConfigurationRuntime> {
        if (cloud.rootDomain == null) {
            "S3 service needs a valid DNS configuration for host based bucket access, ensure that the clouds `rootDomain` is configured".let {
                logError(it)
                return Error(it)
            }
        }

        configuration.buckets.forEach { bucket ->
            if (configuration.buckets.count { bucket.name == it.name } > 1) {
                return Error(
                    "duplicated configuration for bucket with name '${bucket.name}', ensure that the bucket names are unique",
                )
            }

            bucket.accessKeys.forEach { accessKey ->
                if (bucket.accessKeys.count { accessKey.name == it.name } > 1) {
                    return Error(
                        "duplicated access key with name '${accessKey.name}' found for bucket '${bucket.name}', ensure that the access key names are unique",
                    )
                }
            }
        }

        return Success(
            S3ServiceConfigurationRuntime(
                index,
                configuration.common.toRuntime(),
                BackupRuntime.fromConfig(configuration.backup),
                configuration.buckets.map { bucket ->
                    val manuallyManagedPublicAccessDomains = mutableSetOf<String>()
                    val managedPublicAccessDomains = mutableMapOf<String, String>()

                    val dnsZones: List<HetznerDnsZoneLookup> = runBlocking {
                        context.list(HetznerDnsZoneLookup::class)
                    }
                    log.info("validating bucket configuration for bucket '${bucket.name}'")
                    val bucketLog = log.indent()
                    bucket.accessKeys.forEach { accessKey ->
                        bucketLog.info("found access key config '${accessKey.name}'")
                    }

                    bucket.publicAccessDomains.forEach { publicAccessDomain ->
                        val matchingDnsZones = dnsZones.filter { publicAccessDomain.endsWith(it.name) }

                        if (matchingDnsZones.isNotEmpty()) {
                            managedPublicAccessDomains[
                                publicAccessDomain.removeSuffix(matchingDnsZones.single().name).removeSuffix("."),
                            ] = matchingDnsZones.single().name
                        } else {
                            bucketLog.indent().warning("public access domain '$publicAccessDomain' is not managed by any cloud provider, records must me manually created/updated")
                            manuallyManagedPublicAccessDomains.add(publicAccessDomain)
                        }
                    }

                    S3ServiceBucketConfigurationRuntime(
                        bucket.name,
                        bucket.publicAccess,
                        bucket.accessKeys.map { S3ServiceBucketAccessKeyConfigurationRuntime(it.name, it.owner, it.read, it.write) },
                        managedPublicAccessDomains,
                        manuallyManagedPublicAccessDomains,
                    )
                },
            ),
        )
    }

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: S3ServiceConfigurationRuntime, context: SSHProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            listOf(ServerInfo(sshConnectCommand(context, cloud, runtime, 0))),
            listOf(
                EndpointInfo("s3", "https://${s3Host(serviceRootDomain(cloud, runtime))}"),
                EndpointInfo("admin", "https://${s3AdminHost(serviceRootDomain(cloud, runtime))}"),
            ),
        ),
    )

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: S3ServiceConfigurationRuntime, context: SSHProvisionerContext) = markdown {
        h2("Servers")
        text("to access server **${serverName(cloud.environmentContext, runtime.name, 0)}** via SSH, run")
        codeBlock(
            sshConnectCommand(context, cloud, runtime, 0),
        )

        h2("Endpoints")

        list {
            item("GarageFs S3 endpoint: https://${s3Host(serviceRootDomain(cloud, runtime))}")
            item(
                "GarageFs admin endpoint: https://${s3AdminHost(serviceRootDomain(cloud, runtime))}",
            )
        }

        h2("Secrets")
        bold("GarageFS admin secret")
        codeBlock(
            "export ADMIN_SECRET=\"\$(pass ${secretPath(cloud.environmentContext, runtime, listOf("garage", "admin_token"))})\"",
        )

        bold("GarageFS metrics secret")
        codeBlock(
            "export METRICS_SECRET=\"\$(pass ${secretPath(cloud.environmentContext, runtime, listOf("garage", "metrics_token"))})\"",
        )

        h2("Usage examples")

        runtime.buckets.forEach { bucket ->

            bucket.accessKeys.forEach { accessKey ->

                bold("Bucket '${bucket.name}' with access key '${bucket.name}'")

                codeBlock(
                    """
            export ACCESS_KEY="$(pass ${secretPath(cloud.environmentContext, runtime, listOf("buckets", bucket.name, accessKey.name, "access_key"))})"
            export SECRET_KEY="$(pass ${secretPath(cloud.environmentContext, runtime, listOf("buckets", bucket.name, accessKey.name, "secret_key"))})"
            export S3_HOST="$(pass ${secretPath(cloud.environmentContext, runtime, listOf("endpoints", "s3_host"))})"

            s3cmd --host-bucket "%(bucket).${'$'}{S3_HOST}" \
                --host "${'$'}{S3_HOST}" \
                --access_key "${'$'}{ACCESS_KEY}" \
                --secret_key "${'$'}{SECRET_KEY}" \
                ls s3://${bucket.name}

                    """.trimIndent(),
                )
            }
        }
        // TODO add upload example sync --no-mime-magic --guess-mime-type
    }.let { Success(it) }

    override val supportedConfiguration = S3ServiceConfiguration::class

    override val supportedRuntime = S3ServiceConfigurationRuntime::class
}
