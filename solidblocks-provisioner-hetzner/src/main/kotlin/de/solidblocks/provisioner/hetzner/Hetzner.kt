package de.solidblocks.provisioner.hetzner

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.hetzner.cloud.floatingip.HetznerFloatingIpAssignmentResourceProvisioner
import de.solidblocks.provisioner.hetzner.cloud.floatingip.HetznerFloatingIpResourceProvisioner
import de.solidblocks.provisioner.hetzner.cloud.network.HetznerNetworkResourceProvisioner
import de.solidblocks.provisioner.hetzner.cloud.network.HetznerSubnetResourceProvisioner
import de.solidblocks.provisioner.hetzner.cloud.server.HetznerServerResourceProvisioner
import de.solidblocks.provisioner.hetzner.cloud.server.UserDataResourceLookupProvider
import de.solidblocks.provisioner.hetzner.cloud.ssh.HetznerSshResourceProvisioner
import de.solidblocks.provisioner.hetzner.cloud.volume.HetznerVolumeResourceProvisioner
import de.solidblocks.provisioner.hetzner.dns.record.HetznerDnsRecordResourceProvisioner
import de.solidblocks.provisioner.hetzner.dns.zone.HetznerDnsZoneResourceProvisioner
import io.pelle.hetzner.HetznerDnsAPI
import me.tomsdevsn.hetznercloud.HetznerCloudAPI

object Hetzner {

    const val HETZNER_CLOUD_API_TOKEN_RO_KEY = "hetzner_cloud_api_key_ro"
    const val HETZNER_CLOUD_API_TOKEN_RW_KEY = "hetzner_cloud_api_key_rw"

    const val HETZNER_DNS_API_TOKEN_RW_KEY = "hetzner_dns_api_key_rw"

    fun createCloudApi(environment: EnvironmentEntity): HetznerCloudAPI {
        return HetznerCloudAPI(environment.getConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY))
    }

    fun createDnsApi(environment: EnvironmentEntity): HetznerDnsAPI {
        return HetznerDnsAPI(environment.getConfigValue(HETZNER_DNS_API_TOKEN_RW_KEY))
    }

    fun registerLookups(
        provisionerRegistry: ProvisionerRegistry,
        provisioner: InfrastructureProvisioner
    ) {
        provisionerRegistry.addLookupProvider(
            UserDataResourceLookupProvider(provisioner) as IResourceLookupProvider<IResourceLookup<Any>, Any>
        )
    }

    fun registerProvisioners(
        provisionerRegistry: ProvisionerRegistry,
        environment: EnvironmentEntity,
        provisioner: InfrastructureProvisioner
    ) {

        provisionerRegistry.addLookupProvider(
            HetznerDnsZoneResourceProvisioner(
                createDnsApi(environment),
            ) as IResourceLookupProvider<IResourceLookup<Any>, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerDnsRecordResourceProvisioner(
                createDnsApi(environment),
                provisioner
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerSubnetResourceProvisioner(
                createCloudApi(environment),
                provisioner
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerNetworkResourceProvisioner(
                createCloudApi(environment),
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerServerResourceProvisioner(
                createCloudApi(environment),
                provisioner
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerSshResourceProvisioner(
                createCloudApi(environment),
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerVolumeResourceProvisioner(
                createCloudApi(environment),
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerFloatingIpAssignmentResourceProvisioner(
                createCloudApi(environment),
                provisioner
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            HetznerFloatingIpResourceProvisioner(
                createCloudApi(environment)
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )
    }
}
