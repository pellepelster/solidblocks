package de.solidblocks.cloud.services.s3

import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKey
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucket
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermission
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.DnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.Volume
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketConfigurationRuntime
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.GarageFsUserData
import de.solidblocks.utils.LogContext

class S3ServiceConfigurationManager(val cloudConfiguration: CloudConfigurationRuntime) :
    ServiceConfigurationManager<S3ServiceConfiguration, S3ServiceConfigurationRuntime> {

    override fun createResources(
        runtime: S3ServiceConfigurationRuntime
    ): List<InfrastructureResource<*>> {
        val volume = Volume(serverName(cloudConfiguration, runtime.name), cloudConfiguration.hetznerProviderConfig().defaultLocation, 32, emptyMap())
        val adminToken =
            PassSecret(
                secretPath(cloudConfiguration, runtime, listOf("garage", "admin_token")),
                length = 64,
                allowedChars = ('a'..'f') + ('0'..'9'),
            )

        val rpcSecret =
            PassSecret(
                secretPath(cloudConfiguration, runtime, listOf("garage", "rpc_secret")),
                length = 64,
                allowedChars = ('a'..'f') + ('0'..'9'),
            )

        val metricsToken =
            PassSecret(
                secretPath(cloudConfiguration, runtime, listOf("garage", "metrics_token")),
                length = 64,
                allowedChars = ('a'..'f') + ('0'..'9'),
            )

        var s3Host: String? = null

        val userData =
            UserData(
                setOf(volume, adminToken, rpcSecret, metricsToken),
                { context ->
                    if (listOf(adminToken, rpcSecret, metricsToken).any { context.lookup(it.asLookup()) == null }) {
                        return@UserData null
                    }

                    GarageFsUserData(
                        context.ensureLookup(volume.asLookup()).device,
                        "${serverName(cloudConfiguration, runtime.name)}.${cloudConfiguration.rootDomain}",
                        context.ensureLookup(rpcSecret.asLookup()).secret,
                        context.ensureLookup(adminToken.asLookup()).secret,
                        context.ensureLookup(metricsToken.asLookup()).secret,
                        runtime.buckets.map {
                            de.solidblocks.garagefs.GarageFsBucket(it.name, emptyList())
                        },
                        true,
                    ).also {
                        s3Host = it.s3Host
                    }.render()
                },
            )

        val server =
            HetznerServer(
                serverName(cloudConfiguration, runtime.name),
                userData = userData,
                location = cloudConfiguration.hetznerProviderConfig().defaultLocation,
                sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloudConfiguration))),
                volumes = setOf(volume.asLookup()),
                extraDependsOn = setOf(volume),
                type = cloudConfiguration.hetznerProviderConfig().defaultInstanceType
            )

        val zone = DnsZoneLookup(cloudConfiguration.rootDomain)
        val rootDomain =
            HetznerDnsRecord(
                serverName(cloudConfiguration, runtime.name),
                zone,
                listOf(server.asLookup()),
            )
        val catchAllDomain =
            HetznerDnsRecord(
                "*.${serverName(cloudConfiguration, runtime.name)}",
                zone,
                listOf(server.asLookup()),
            )

        val bucketResources = mutableListOf<InfrastructureResource<*>>()

        runtime.buckets.forEach {
            val accessKey = GarageFsAccessKey(secretPath(cloudConfiguration, runtime, listOf("owner", "access_key")), server, adminToken)
            bucketResources.add(accessKey)

            bucketResources.add(PassSecret(secretPath(cloudConfiguration, runtime, listOf("owner", "secret_access_key")), secret = {
                it.ensureLookup(accessKey.asLookup()).secretAccessKey
            }, dependsOn = setOf(accessKey)))

            bucketResources.add(PassSecret(secretPath(cloudConfiguration, runtime, listOf("owner", "access_key_id")), secret = {
                it.ensureLookup(accessKey.asLookup()).id
            }, dependsOn = setOf(accessKey)))

            val bucket = GarageFsBucket(it.name, server, adminToken, websiteAccess = it.publicAccess)
            bucketResources.add(bucket)

            val permission = GarageFsPermission(
                bucket,
                accessKey,
                server,
                adminToken,
                true,
                true,
                true
            )
            bucketResources.add(permission)
        }

        val s3HostSecret = PassSecret(secretPath(cloudConfiguration, runtime, listOf("endpoints", "s3_host")), secret = {
            s3Host ?: "unknown_s3_host"
        })

        return listOf(server, volume, rootDomain, catchAllDomain, adminToken, rpcSecret, metricsToken) + bucketResources + listOf(s3HostSecret)
    }

    override fun createProvisioners(runtime: S3ServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>(GarageFsBucketProvisioner(), GarageFsAccessKeyProvisioner(), GarageFsPermissionProvisioner())

    override fun validatConfiguration(
        configuration: S3ServiceConfiguration,
        context: LogContext,
    ): Result<S3ServiceConfigurationRuntime> {
        configuration.buckets.forEach { bucket ->
            if (configuration.buckets.count { bucket.name == it.name } > 1) {
                return Error("duplicated configuration for bucket '${bucket.name}'")
            }
        }

        return Success(
            S3ServiceConfigurationRuntime(
                configuration.name,
                configuration.buckets.map {
                    S3ServiceBucketConfigurationRuntime(it.name, it.publicAccess)
                },
            ),
        )
    }

    override val supportedConfiguration = S3ServiceConfiguration::class

    override val supportedRuntime = S3ServiceConfigurationRuntime::class
}
