package de.solidblocks.cloud.services.s3

import de.solidblocks.cloud.Constants.secretPath
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
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logWarning

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
                        de.solidblocks.garagefs.GarageFsBucket(it.name, emptyList())
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
            type = cloudConfiguration.hetznerProviderConfig().defaultInstanceType
        )

        val dnsResources = if (cloudConfiguration.rootDomain == null) {
            emptyList()
        } else {
            val zone = HetznerDnsZoneLookup(cloudConfiguration.rootDomain)
            val rootDomain = HetznerDnsRecord(
                serverName(cloudConfiguration, runtime.name),
                zone,
                listOf(server.asLookup()),
            )
            val catchAllDomain = HetznerDnsRecord("*.${serverName(cloudConfiguration, runtime.name)}", zone, listOf(server.asLookup()))

            listOf(rootDomain, catchAllDomain)
        }

        val layout = GarageFsLayout(runtime.dataVolumeSize.bytes, server, adminToken)

        val bucketResources = mutableListOf<BaseInfrastructureResource<*>>()

        runtime.buckets.forEach {
            val accessKey = GarageFsAccessKey(secretPath(cloudConfiguration, runtime, listOf("owner", "access_key")), server, adminToken, setOf(layout))
            bucketResources.add(accessKey)

            bucketResources.add(PassSecret(secretPath(cloudConfiguration, runtime, listOf("owner", "secret_access_key")), secret = {
                it.ensureLookup(accessKey.asLookup()).secretAccessKey
            }, dependsOn = setOf(accessKey)))

            bucketResources.add(PassSecret(secretPath(cloudConfiguration, runtime, listOf("owner", "access_key_id")), secret = {
                it.ensureLookup(accessKey.asLookup()).id
            }, dependsOn = setOf(accessKey)))

            val bucket = GarageFsBucket(it.name, server, adminToken, websiteAccess = it.publicAccess, emptyList(), setOf(layout))
            bucketResources.add(bucket)

            val permission = GarageFsPermission(
                bucket, accessKey, server, adminToken, true, true, true, setOf(layout)
            )
            bucketResources.add(permission)
        }

        val s3HostSecret = PassSecret(secretPath(cloudConfiguration, runtime, listOf("endpoints", "s3_host")), secret = {
            GarageFsUserData.s3Host(serviceRootDomain(runtime))
        })

        return listOf(server, volume, adminToken, rpcSecret, metricsToken, layout) + bucketResources + listOf(s3HostSecret) + dnsResources
    }

    override fun createProvisioners(runtime: S3ServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>(GarageFsBucketProvisioner(), GarageFsAccessKeyProvisioner(), GarageFsPermissionProvisioner(), GarageFsLayoutProvisioner())

    override fun validatConfiguration(configuration: S3ServiceConfiguration, context: ProvisionerContext, log: LogContext): Result<S3ServiceConfigurationRuntime> {

        val manuallyManagedPublicAccessDomains = mutableSetOf<String>()

        configuration.buckets.forEach { bucket ->
            if (configuration.buckets.count { bucket.name == it.name } > 1) {
                return Error("duplicated configuration for bucket with name '${bucket.name}', ensure that the bucket names are unique")
            }

            bucket.accessKeys.forEach { accessKey ->
                if (bucket.accessKeys.count { accessKey.name == it.name } > 1) {
                    return Error("duplicated access key with name '${accessKey.name}' found for bucket '${bucket.name}', ensure that the access key names are unique")
                }
            }

            logInfo("validating bucket configuration for bucket '${bucket.name}'", context = log)
            bucket.publicAccessDomains.forEach { domain ->
                if (context.lookup(HetznerDnsZoneLookup(domain)) == null) {
                    logWarning("public access domain '$domain' is not managed by any cloud provider, records must me manually updated", context = log.indent())
                    manuallyManagedPublicAccessDomains.add(domain)
                }
            }
        }

        return Success(
            S3ServiceConfigurationRuntime(
                configuration.name, ByteSize.fromGigabytes(configuration.dataVolumeSize), configuration.buckets.map {
                    S3ServiceBucketConfigurationRuntime(it.name, it.publicAccess, it.accessKeys.map {
                        S3ServiceBucketAccessKeyConfigurationRuntime(it.name)
                    })
                }, manuallyManagedPublicAccessDomains
            ),
        )
    }

    override fun output(
        runtime: S3ServiceConfigurationRuntime,
        context: ProvisionerContext
    ): Result<List<Output>> {
        return Success(listOf(Output("service '${runtime.name}'", """

**Endpoints**

* GarageFs S3 endpoint: ${GarageFsUserData.s3Host(serviceRootDomain(runtime))}
* GarageFs admin endpoint: ${GarageFsUserData.s3AdminHost(serviceRootDomain(runtime))}
            
        """.trimIndent())))
    }

    override val supportedConfiguration = S3ServiceConfiguration::class

    override val supportedRuntime = S3ServiceConfigurationRuntime::class
}
