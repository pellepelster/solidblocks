package de.solidblocks.cloud.services.s3

import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucket
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.DnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.Volume
import de.solidblocks.cloud.provisioner.pass.Secret
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

class S3ServiceConfigurationManager(val cloudConfiguration: CloudConfiguration) :
    ServiceConfigurationManager<S3ServiceConfiguration, S3ServiceConfigurationRuntime> {

  override fun createResources(
      runtime: S3ServiceConfigurationRuntime
  ): List<InfrastructureResource<*, *>> {
    val volume = Volume(serverName(cloudConfiguration, runtime.name), "hel1", 32, emptyMap())
    val adminToken =
        Secret(
            "${runtime.name}/garage_admin_token",
            length = 64,
            allowedChars = ('a'..'f') + ('0'..'9'),
        )

    val userData =
        UserData(
            setOf(volume, adminToken),
            {
              GarageFsUserData(
                      it.ensureLookup(volume.asLookup()).device,
                      "${serverName(cloudConfiguration, runtime.name)}.${cloudConfiguration.rootDomain}",
                      it.ensureLookup(adminToken.asLookup()).secret,
                      it.ensureLookup(adminToken.asLookup()).secret,
                      it.ensureLookup(adminToken.asLookup()).secret,
                      runtime.buckets.map {
                        de.solidblocks.garagefs.GarageFsBucket(it.name, emptyList())
                      },
                      true,
                  )
                  .render()
            },
        )

    val server =
        HetznerServer(
            serverName(cloudConfiguration, runtime.name),
            userData = userData,
            location = "hel1",
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloudConfiguration))),
            volumes = setOf(volume.asLookup()),
            extraDependsOn = setOf(volume),
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

    val buckets =
        runtime.buckets.map {
          GarageFsBucket(it.name, server, adminToken, websiteAccess = it.publicAccess)
        }

    return listOf(server, volume, rootDomain, catchAllDomain, adminToken) + buckets
  }

  override fun createProvisioners(runtime: S3ServiceConfigurationRuntime) =
      listOf(GarageFsBucketProvisioner())

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
