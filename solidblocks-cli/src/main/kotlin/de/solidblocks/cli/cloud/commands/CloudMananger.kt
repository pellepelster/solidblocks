package de.solidblocks.cli.cloud.commands

import de.solidblocks.api.resources.dns.DnsRecord
import de.solidblocks.api.resources.dns.DnsZone
import de.solidblocks.api.resources.dns.DnsZoneRuntime
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.compute.UserDataDataSource
import de.solidblocks.api.resources.infrastructure.compute.Volume
import de.solidblocks.api.resources.infrastructure.compute.VolumeRuntime
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.api.resources.infrastructure.network.FloatingIpAssignment
import de.solidblocks.api.resources.infrastructure.network.FloatingIpRuntime
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.utils.Base64Encode
import de.solidblocks.api.resources.infrastructure.utils.ConstantDataSource
import de.solidblocks.api.resources.infrastructure.utils.ResourceLookup
import de.solidblocks.cli.Contants.BACKUP_POLICY_NAME
import de.solidblocks.cli.Contants.CONTROLLER_POLICY_NAME
import de.solidblocks.cli.Contants.hostSshMountName
import de.solidblocks.cli.Contants.kvMountName
import de.solidblocks.cli.Contants.pkiMountName
import de.solidblocks.cli.Contants.userSshMountName
import de.solidblocks.cloud.config.CloudConfig
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.CloudEnvironmentConfig
import de.solidblocks.core.IDataSource
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.HetznerCloudCredentialsProvider
import de.solidblocks.provisioner.hetzner.cloud.getHetznerCloudApiToken
import de.solidblocks.provisioner.hetzner.dns.HetznerDnsCredentialsProvider
import de.solidblocks.provisioner.hetzner.dns.getHetznerDnsApiToken
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRole
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRole
import mu.KotlinLogging
import org.apache.commons.net.util.SubnetUtils
import org.springframework.stereotype.Component
import org.springframework.vault.support.Policy

@Component
class CloudMananger(
        val cloudCredentialsProvider: HetznerCloudCredentialsProvider,
        val dnsCredentialsProvider: HetznerDnsCredentialsProvider,
        val provisioner: Provisioner,
        val cloudConfigurationManager: CloudConfigurationManager
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(environment: String, rootDomain: String, hetznerCloudApiToken: String, hetznerDnsApiToken: String) {
        cloudCredentialsProvider.addApiToken(hetznerCloudApiToken)
        dnsCredentialsProvider.addApiToken(hetznerDnsApiToken)

        //createCloudModel(environment, rootDomain)

        provisioner.destroyAll()
    }

    fun show(name: String): Boolean {
        return true
    }

    fun bootstrap(cloudName: String, environmentName: String): Boolean {
        val cloudConfig = cloudConfigurationManager.cloudByName(cloudName)
        if (cloudConfig == null) {
            logger.error { "cloud '${cloudName}' not found" }
            return false
        }

        val environment = cloudConfig.environments.filter { it.name == environmentName }.firstOrNull()
        if (environment == null) {
            logger.error { "cloud '${cloudName}' has no environment '${environment}'" }
            return false
        }


        cloudCredentialsProvider.addApiToken(environment.configValues.getHetznerCloudApiToken()!!.value)
        dnsCredentialsProvider.addApiToken(environment.configValues.getHetznerDnsApiToken()!!.value)

        createCloudModel(cloudConfig, environment, setOf(SshKey(environment.name, environment.sshConfig.sshPublicKey)))

        return provisioner.apply()
    }

    private fun createCloudModel(
            cloudConfig: CloudConfig,
            environmentConfig: CloudEnvironmentConfig,
            sshKeys: Set<SshKey> = emptySet()
    ) {
        val rootZone = DnsZone(cloudConfig.rootDomain)

        val id = "solidblocks"
        //val rootZone = DnsZone(cloud.solidblocksConfig.domain)

        val subnet = SubnetUtils("10.0.0.0/32").info
        val network = Network(id, subnet)

        val networkLayer = provisioner.createLayer("networkLayer")
        networkLayer.addResource(network)

        val location = "nbg1"

        addVaultInfraLayer(location, environmentConfig, rootZone, cloudConfig, network, sshKeys)

        addVaultLayer(cloudConfig, environmentConfig)


        addBackupLayer(location, environmentConfig, rootZone, cloudConfig, network, sshKeys)
    }

    private fun addVaultInfraLayer(location: String, environmentConfig: CloudEnvironmentConfig, rootZone: DnsZone, cloudConfig: CloudConfig, network: Network, sshKeys: Set<SshKey>) {
        val vaultInfraLayer = provisioner.createLayer("vaultInfrastructureLayer")
        val vault1Volume = Volume("vault-1_${location}", location)
        vaultInfraLayer.addResource(vault1Volume)
        val vault1FloatingIp = FloatingIp("vault-1", location, mapOf("role" to "vault", "name" to "vault-1"))

        val vault1Record = DnsRecord("vault-1.${environmentConfig.name}", vault1FloatingIp, zone = rootZone)
        vaultInfraLayer.addResource(vault1Record)

        val vaultRecord = DnsRecord("vault.${environmentConfig.name}", vault1FloatingIp, zone = rootZone)
        vaultInfraLayer.addResource(vaultRecord)

        val variables = HashMap<String, IDataSource<String>>()
        variables["cloud_name"] = ConstantDataSource(cloudConfig.name)
        variables["cloud_root_domain"] = ResourceLookup<DnsZoneRuntime>(rootZone) {
            it.name
        }
        variables["environment_name"] = ConstantDataSource(environmentConfig.name)
        variables["hostname"] = ConstantDataSource("vault-1")
        variables["public_ip"] = ResourceLookup<FloatingIpRuntime>(vault1FloatingIp) {
            it.ipv4
        }
        variables["ssh_identity_ed25519_key"] = Base64Encode(ConstantDataSource(environmentConfig.sshConfig.sshIdentityPrivateKey))
        variables["ssh_identity_ed25519_pub"] = Base64Encode(ConstantDataSource(environmentConfig.sshConfig.sshIdentityPublicKey))

        variables["storage_local_device"] = ResourceLookup<VolumeRuntime>(vault1Volume) {
            it.device
        }


        val userData = UserDataDataSource("lib-cloud-init-generated/vault-cloud-init.sh", variables)
        val vault1Server = Server(
                "vault-1",
                network,
                userData,
                sshKeys = sshKeys,
                location = location,
                volume = vault1Volume,
                dependencies = listOf(vault1Record)
        )
        vaultInfraLayer.addResource(vault1Server)
        vaultInfraLayer.addResource(FloatingIpAssignment(server = vault1Server, floatingIp = vault1FloatingIp))
    }

    private fun addBackupLayer(location: String, environmentConfig: CloudEnvironmentConfig, rootZone: DnsZone, cloudConfig: CloudConfig, network: Network, sshKeys: Set<SshKey>) {
        val backupInfraLayer = provisioner.createLayer("backupInfrastructureLayer")

        val backupVolume = Volume("backup_${location}", location)
        backupInfraLayer.addResource(backupVolume)

        val backupFloatingIp = FloatingIp("backup", location, mapOf("role" to "backup", "name" to "backup"))
        backupInfraLayer.addResource(backupFloatingIp)

        val dnsRecord = DnsRecord("backup.${environmentConfig.name}", backupFloatingIp, zone = rootZone)
        backupInfraLayer.addResource(dnsRecord)

        val variables = HashMap<String, IDataSource<String>>()
        variables["cloud_name"] = ConstantDataSource(cloudConfig.name)
        variables["cloud_root_domain"] = ResourceLookup<DnsZoneRuntime>(rootZone) {
            it.name
        }
        variables["environment_name"] = ConstantDataSource(environmentConfig.name)
        variables["hostname"] = ConstantDataSource("backup")
        variables["public_ip"] = ResourceLookup<FloatingIpRuntime>(backupFloatingIp) {
            it.ipv4
        }
        variables["ssh_identity_ed25519_key"] = Base64Encode(ConstantDataSource(environmentConfig.sshConfig.sshIdentityPrivateKey))
        variables["ssh_identity_ed25519_pub"] = Base64Encode(ConstantDataSource(environmentConfig.sshConfig.sshIdentityPublicKey))

        variables["storage_local_device"] = ResourceLookup<VolumeRuntime>(backupVolume) {
            it.device
        }


        val backupUserData = UserDataDataSource("lib-cloud-init-generated/backup-cloud-init.sh", variables)
        val backupServer = Server(
                "backup",
                network,
                backupUserData,
                sshKeys = sshKeys,
                location = location,
                volume = backupVolume,
                dependencies = listOf(dnsRecord)
        )
        backupInfraLayer.addResource(backupServer)
        backupInfraLayer.addResource(FloatingIpAssignment(server = backupServer, floatingIp = backupFloatingIp))
    }

    private fun addVaultLayer(cloud: CloudConfig,
                              environment: CloudEnvironmentConfig) {

        val vaultLayer = provisioner.createLayer("vaultConfigLayer")
        provisioner.addProvider(VaultRootClientProvider(cloud.name, environment.name, "https://vault.${environment.name}.${cloud.rootDomain}:8200", cloudConfigurationManager))

        val hostPkiMount = VaultMount(pkiMountName(cloud, environment), "pki")
        val hostPkiBackendRole = VaultPkiBackendRole(
                name = pkiMountName(cloud, environment),
                allowAnyName = true,
                generateLease = true,
                maxTtl = "168h",
                ttl = "168h",
                keyBits = 521,
                keyType = "ec",
                mount = hostPkiMount
        )
        vaultLayer.addResource(hostPkiBackendRole)

        val hostSshMount = VaultMount(hostSshMountName(cloud, environment),
                "ssh")
        val hostSshBackendRole = VaultSshBackendRole(
                name = hostSshMountName(cloud, environment),
                keyType = "ca",
                maxTtl = "168h",
                ttl = "168h",
                allowHostCertificates = true,
                allowUserCertificates = false,
                mount = hostSshMount
        )
        vaultLayer.addResource(hostSshBackendRole)

        val kvMount = VaultMount(kvMountName(cloud, environment), "kv-v2")
        vaultLayer.addResource(kvMount)

        /*
        val solidblocksConfig =
                VaultKV("solidblocks/cloud/config",
                        JacksonUtils.toMap(cloud.solidblocksConfig), kvMount)
        vaultLayer.addResource(solidblocksConfig)

        val hetznerProviderConfig = VaultKV(
                "solidblocks/providers/hetzner",

                JacksonUtils.toMap(HetznerProviderConfig(cloud.configurations.getHetzne
                        rCloudApiToken ()?.value!!)),
                kvMount
        )
        */

        val controllerPolicy = VaultPolicy(
                CONTROLLER_POLICY_NAME,
                setOf(

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/ config").capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/providers/hetzner")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path("${pkiMountName(cloud, environment)}/issue/${pkiMountName(cloud, environment)}")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE)
                                .build(),

                        Policy.Rule.builder().path("${userSshMountName(cloud, environment)}/sign/${userSshMountName(cloud, environment)}")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE,
                                        Policy.BuiltinCapabilities.CREATE)
                                .build(),

                        Policy.Rule.builder().path("${userSshMountName(cloud, environment)}/config/ca")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path("${hostSshMountName(cloud, environment)}/sign/${hostSshMountName(cloud, environment)}")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE,
                                        Policy.BuiltinCapabilities.CREATE)
                                .build(),

                        Policy.Rule.builder().path("${hostSshMountName(cloud, environment)}/config/ca")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),
                ),
        )
        vaultLayer.addResource(controllerPolicy)

        val backupPolicy = VaultPolicy(
                BACKUP_POLICY_NAME,
                setOf(

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/config")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path("${pkiMountName(cloud, environment)}/issue/${pkiMountName(cloud, environment)}")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE)
                                .build(),

                        Policy.Rule.builder().path("${userSshMountName(cloud, environment)}/sign/${userSshMountName(cloud, environment)}")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE,
                                        Policy.BuiltinCapabilities.CREATE)
                                .build(),

                        Policy.Rule.builder().path("${userSshMountName(cloud, environment)}/config/ca")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path("${hostSshMountName(cloud, environment)}/sign/${hostSshMountName(cloud, environment)}")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE,
                                        Policy.BuiltinCapabilities.CREATE)
                                .build(),

                        Policy.Rule.builder().path("${hostSshMountName(cloud, environment)}/config/ca")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        ),
        )
        vaultLayer.addResource(backupPolicy)
    }


}
