package de.solidblocks.cloud.services.s3

import de.solidblocks.cloud.Constants
import de.solidblocks.cloud.Constants.DEFAULT_NETWORK
import de.solidblocks.cloud.Constants.DEFAULT_SERVICE_SUBNET
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.Constants.serverIp
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.Output
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKey
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucket
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayout
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayoutProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermission
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketAccessKeyConfigurationRuntime
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketConfigurationRuntime
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationRuntime
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.GarageFsUserData
import de.solidblocks.garagefs.GarageFsUserData.Companion.s3AdminHost
import de.solidblocks.garagefs.GarageFsUserData.Companion.s3Host
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logWarning
import kotlinx.coroutines.runBlocking

class S3ServiceConfigurationManager(val cloudConfiguration: CloudConfigurationRuntime) : ServiceConfigurationManager<S3ServiceConfiguration, S3ServiceConfigurationRuntime> {

    private fun serviceRootDomain(runtime: ServiceConfigurationRuntime) = "${serverName(cloudConfiguration, runtime.name)}.${cloudConfiguration.rootDomain}"

    override fun createResources(runtime: S3ServiceConfigurationRuntime): List<BaseInfrastructureResource<*>> {

        val volume = HetznerVolume(serverName(cloudConfiguration, runtime.name), cloudConfiguration.hetznerProviderConfig().defaultLocation, runtime.dataVolumeSize, emptyMap())

        val adminToken = PassSecret(
            secretPath(cloudConfiguration, runtime, listOf("garage", "admin_token")),
            length = 64,
            allowedChars = ('a'..'f') + ('0'..'9'),
        )

        val rpcSecret = PassSecret(
            secretPath(cloudConfiguration, runtime, listOf("garage", "rpc_secret")),
            length = 64,
            allowedChars = ('a'..'f') + ('0'..'9'),
        )

        val metricsToken = PassSecret(
            secretPath(cloudConfiguration, runtime, listOf("garage", "metrics_token")),
            length = 64,
            allowedChars = ('a'..'f') + ('0'..'9'),
        )

        val userData = UserData(
            setOf(volume, adminToken, rpcSecret, metricsToken),
            { context ->
                if (listOf(adminToken, rpcSecret, metricsToken).any { context.lookup(it.asLookup()) == null }) {
                    return@UserData null
                }

                GarageFsUserData(
                    context.ensureLookup(volume.asLookup()).device,
                    serviceRootDomain(runtime),
                    context.ensureLookup(rpcSecret.asLookup()).secret,
                    context.ensureLookup(adminToken.asLookup()).secret,
                    context.ensureLookup(metricsToken.asLookup()).secret,
                    runtime.buckets.map {
                        de.solidblocks.garagefs.GarageFsBucket(it.name, it.managedPublicWebAccessDomains.values.toSet() + it.manuallyManagedPublicWebAccessDomains)
                    },
                    true,
                ).render()
            },
        )

        val server = HetznerServer(
            serverName(cloudConfiguration, runtime.name),
            userData = userData,
            location = cloudConfiguration.hetznerProviderConfig().defaultLocation,
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloudConfiguration))),
            volumes = setOf(volume.asLookup()),
            dependsOn = setOf(volume),
            type = cloudConfiguration.hetznerProviderConfig().defaultInstanceType,
            subnet = HetznerSubnetLookup(DEFAULT_SERVICE_SUBNET, HetznerNetworkLookup(networkName(cloudConfiguration))),
            privateIp = serverIp(runtime.index)
        )

        if (cloudConfiguration.rootDomain == null) {
            throw IllegalArgumentException("root domain is required")

        }

        val zone = HetznerDnsZoneLookup(cloudConfiguration.rootDomain)
        val rootDomain = HetznerDnsRecord(
            serverName(cloudConfiguration, runtime.name),
            zone,
            listOf(server.asLookup()),
        )

        val catchAllDomain = HetznerDnsRecord("*.${serverName(cloudConfiguration, runtime.name)}", zone, listOf(server.asLookup()))
        val dnsResources = runtime.buckets.flatMap { it.managedPublicWebAccessDomains.entries }.map {
            if (it.key.isEmpty()) {
                HetznerDnsRecord(
                    "@", HetznerDnsZoneLookup(it.value), listOf(server.asLookup())
                )
            } else {
                HetznerDnsRecord(
                    it.value, HetznerDnsZoneLookup(it.value), listOf(server.asLookup())
                )
            }
        } + listOf(rootDomain, catchAllDomain)

        val layout = GarageFsLayout(runtime.dataVolumeSize.bytes, server, adminToken)
        val bucketResources = mutableListOf<BaseInfrastructureResource<*>>()

        runtime.buckets.forEach {

            val bucket = GarageFsBucket(it.name, server, adminToken, websiteAccess = it.publicAccess, emptyList(), setOf(layout))
            bucketResources.add(bucket)

            it.accessKeys.forEach { accessKeyRuntime ->

                val accessKey = GarageFsAccessKey(secretPath(cloudConfiguration, runtime, listOf(accessKeyRuntime.name)), server, adminToken, setOf(layout))
                bucketResources.add(accessKey)

                bucketResources.add(PassSecret(secretPath(cloudConfiguration, runtime, listOf(accessKeyRuntime.name, "secret_key")), secret = {
                    it.ensureLookup(accessKey.asLookup()).secretAccessKey
                }, dependsOn = setOf(accessKey)))

                bucketResources.add(PassSecret(secretPath(cloudConfiguration, runtime, listOf(accessKeyRuntime.name, "access_key")), secret = {
                    it.ensureLookup(accessKey.asLookup()).id
                }, dependsOn = setOf(accessKey)))

                val permission = GarageFsPermission(
                    bucket, accessKey, server, adminToken, true, true, true, setOf(layout)
                )
                bucketResources.add(permission)
            }
        }

        val s3HostSecret = PassSecret(secretPath(cloudConfiguration, runtime, listOf("endpoints", "s3_host")), secret = {
            s3Host(serviceRootDomain(runtime))
        })

        return listOf(server, volume, adminToken, rpcSecret, metricsToken, layout) + bucketResources + listOf(s3HostSecret) + dnsResources
    }

    override fun createProvisioners(runtime: S3ServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>(GarageFsBucketProvisioner(), GarageFsAccessKeyProvisioner(), GarageFsPermissionProvisioner(), GarageFsLayoutProvisioner())

    override fun validatConfiguration(index: Int, configuration: S3ServiceConfiguration, context: ProvisionerContext, log: LogContext): Result<S3ServiceConfigurationRuntime> {

        if (cloudConfiguration.rootDomain == null) {
            "S3 service needs a valid DNS configuration for host based bucket access, ensure that the clouds `rootDomain` is configured".let {
                logError(it)
                return Error(it)
            }
        }

        configuration.buckets.forEach { bucket ->
            if (configuration.buckets.count { bucket.name == it.name } > 1) {
                return Error("duplicated configuration for bucket with name '${bucket.name}', ensure that the bucket names are unique")
            }

            bucket.accessKeys.forEach { accessKey ->
                if (bucket.accessKeys.count { accessKey.name == it.name } > 1) {
                    return Error("duplicated access key with name '${accessKey.name}' found for bucket '${bucket.name}', ensure that the access key names are unique")
                }
            }
        }

        return Success(
            S3ServiceConfigurationRuntime(
                index,
                configuration.name, ByteSize.fromGigabytes(configuration.dataVolumeSize), configuration.buckets.map { bucket ->

                    val manuallyManagedPublicAccessDomains = mutableSetOf<String>()
                    val managedPublicAccessDomains = mutableMapOf<String, String>()

                    val dnsZones: List<HetznerDnsZoneRuntime> = runBlocking {
                        context.registry.list(HetznerDnsZoneLookup::class)
                    }

                    logInfo("validating bucket configuration for bucket '${bucket.name}'", context = log)
                    bucket.publicAccessDomains.forEach { publicAccessDomain ->
                        val matchingDnsZones = dnsZones.filter {
                            publicAccessDomain.endsWith(it.name)
                        }

                        if (matchingDnsZones.isNotEmpty()) {
                            managedPublicAccessDomains[publicAccessDomain.removeSuffix(matchingDnsZones.single().name).removeSuffix(".")] = matchingDnsZones.single().name
                        } else {
                            logWarning("public access domain '$publicAccessDomain' is not managed by any cloud provider, records must me manually created/updated", context = log.indent())
                            manuallyManagedPublicAccessDomains.add(publicAccessDomain)
                        }
                    }

                    S3ServiceBucketConfigurationRuntime(bucket.name, bucket.publicAccess, bucket.accessKeys.map {
                        S3ServiceBucketAccessKeyConfigurationRuntime(it.name)
                    }, managedPublicAccessDomains, manuallyManagedPublicAccessDomains)
                })
        )
    }

    override fun output(
        runtime: S3ServiceConfigurationRuntime, context: ProvisionerContext
    ): Result<List<Output>> {
        return Success(
            listOf(
                Output(
                    "S3 Service '${runtime.name}'", """

## Endpoints

* GarageFs S3 endpoint: https://${s3Host(serviceRootDomain(runtime))}
* GarageFs admin endpoint: https://${s3AdminHost(serviceRootDomain(runtime))}

## Secrets

**GarageFS admin secret**
```
export ADMIN_SECRET="$(pass ${secretPath(cloudConfiguration, runtime, listOf("garage", "admin_token"))})"
```

**GarageFS metrics secret**
```
export METRICS_SECRET="$(pass ${secretPath(cloudConfiguration, runtime, listOf("garage", "metrics_token"))})"
```

## Usage examples

${bucketsHelp(runtime)}
""".trimIndent()
                )
            )
        )
    }

    fun bucketsHelp(runtime: S3ServiceConfigurationRuntime) = runtime.buckets.joinToString("\n") {
        bucketHelpAccessKeysHelp(it, runtime, it.accessKeys) + "\n"
    }

    fun bucketHelpAccessKeysHelp(bucket: S3ServiceBucketConfigurationRuntime, runtime: S3ServiceConfigurationRuntime, accessKeys: List<S3ServiceBucketAccessKeyConfigurationRuntime>) =
        accessKeys.joinToString("\n") {
            """
        **Bucket '${bucket.name}' with access key '${it.name}'**
        ```
        export ACCESS_KEY="$(pass ${secretPath(cloudConfiguration, runtime, listOf(it.name, "access_key"))})"
        export SECRET_KEY="$(pass ${secretPath(cloudConfiguration, runtime, listOf(it.name, "secret_key"))})" 
        export S3_HOST="$(pass ${secretPath(cloudConfiguration, runtime, listOf("endpoints", "s3_host"))})" 
        
        s3cmd --host-bucket "%(bucket).${'$'}{S3_HOST} \
            --host ${'$'}{S3_HOST} \
            --access_key ${'$'}{ACCESS_KEY} \
            --secret_key ${'$'}{SECRET_KEY} \
            ls s3://${bucket.name}
        ```
        """.trimIndent()
        }

    //sync --no-mime-magic --guess-mime-type

    override val supportedConfiguration = S3ServiceConfiguration::class

    override val supportedRuntime = S3ServiceConfigurationRuntime::class
}
