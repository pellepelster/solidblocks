package de.solidblocks.cli.cloud.commands

import de.solidblocks.api.resources.dns.DnsRecord
import de.solidblocks.api.resources.dns.DnsZone
import de.solidblocks.api.resources.dns.DnsZoneRuntime
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.compute.UserDataDataSource
import de.solidblocks.api.resources.infrastructure.compute.Volume
import de.solidblocks.api.resources.infrastructure.compute.VolumeRuntime
import de.solidblocks.api.resources.infrastructure.network.*
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.utils.Base64Encode
import de.solidblocks.api.resources.infrastructure.utils.ConstantDataSource
import de.solidblocks.api.resources.infrastructure.utils.CustomDataSource
import de.solidblocks.api.resources.infrastructure.utils.ResourceLookup
import de.solidblocks.base.Constants.ConfigKeys.Companion.GITHUB_TOKEN_RO_KEY
import de.solidblocks.base.Constants.ConfigKeys.Companion.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.base.Constants.ConfigKeys.Companion.HETZNER_CLOUD_API_TOKEN_RW_KEY
import de.solidblocks.base.Constants.ConfigKeys.Companion.HETZNER_DNS_API_TOKEN_RW_KEY
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.cli.Contants.BACKUP_POLICY_NAME
import de.solidblocks.cli.Contants.CONTROLLER_POLICY_NAME
import de.solidblocks.cli.Contants.hostSshMountName
import de.solidblocks.cli.Contants.kvMountName
import de.solidblocks.cli.Contants.pkiMountName
import de.solidblocks.cli.Contants.userSshMountName
import de.solidblocks.cloud.config.CloudConfig
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.CloudEnvironmentConfig
import de.solidblocks.cloud.config.getConfigValue
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.ResourceGroup
import de.solidblocks.provisioner.hetzner.cloud.HetznerCloudCredentialsProvider
import de.solidblocks.provisioner.hetzner.dns.HetznerDnsCredentialsProvider
import de.solidblocks.provisioner.vault.kv.VaultKV
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRole
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRole
import mu.KotlinLogging
import org.apache.commons.net.util.SubnetUtils
import org.springframework.stereotype.Component
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.Policy
import org.springframework.vault.support.VaultTokenRequest
import java.net.URL

@Component
class CloudMananger(
        val cloudCredentialsProvider: HetznerCloudCredentialsProvider,
        val dnsCredentialsProvider: HetznerDnsCredentialsProvider,
        val provisioner: Provisioner,
        val cloudConfigurationManager: CloudConfigurationManager
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(cloudName: String, environmentName: String): Boolean {
        if (!cloudConfigurationManager.hasCloud(cloudName)) {
            logger.error { "cloud '${cloudName}' not found" }
            return false
        }

        val cloudConfig = cloudConfigurationManager.cloudByName(cloudName)

        val environment = cloudConfig.environments.filter { it.name == environmentName }.firstOrNull()
        if (environment == null) {
            logger.error { "cloud '${cloudName}' has no environment '${environment}'" }
            return false
        }

        cloudCredentialsProvider.addApiToken(environment.configValues.getConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY)!!.value)
        dnsCredentialsProvider.addApiToken(environment.configValues.getConfigValue(HETZNER_DNS_API_TOKEN_RW_KEY)!!.value)


        provisioner.destroyAll()
        return true
    }

    fun bootstrap(cloudName: String, environmentName: String): Boolean {

        if (!cloudConfigurationManager.hasCloud(cloudName)) {
            logger.error { "cloud '${cloudName}' not found" }
            return false
        }

        val cloud = cloudConfigurationManager.cloudByName(cloudName)

        val environment = cloud.environments.filter { it.name == environmentName }.firstOrNull()
        if (environment == null) {
            logger.error { "cloud '${cloudName}' has no environment '${environment}'" }
            return false
        }


        cloudCredentialsProvider.addApiToken(environment.configValues.getConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY)!!.value)
        dnsCredentialsProvider.addApiToken(environment.configValues.getConfigValue(HETZNER_DNS_API_TOKEN_RW_KEY)!!.value)

        createCloudModel(cloud, environment, setOf(SshKey("${cloud.name}-${environment.name}", environment.sshConfig.sshPublicKey)))

        return provisioner.apply()
    }

    private fun createCloudModel(
            cloud: CloudConfig,
            environment: CloudEnvironmentConfig,
            sshKeys: Set<SshKey> = emptySet()
    ) {
        val rootZone = DnsZone(cloud.rootDomain)

        val id = "solidblocks"
        //val rootZone = DnsZone(cloud.solidblocksConfig.domain)


        val networkResourceGroup = provisioner.createResourceGroup("network")

        val network = Network(id, SubnetUtils("10.1.0.0/16"))
        networkResourceGroup.addResource(network)

        val subnet = Subnet(SubnetUtils("10.1.1.0/24"), network)
        networkResourceGroup.addResource(subnet)

        val location = "nbg1"

        val vaultInfraResourceGroup = createVaultInfraResourceGroup(location, environment, rootZone, cloud, network, sshKeys)

        createVaultConfigResourceGroup(vaultInfraResourceGroup, cloud, environment)
        addBackupResourceGroup(location, environment, rootZone, cloud, network, sshKeys)
    }

    private fun createVaultInfraResourceGroup(location: String, environment: CloudEnvironmentConfig, rootZone: DnsZone, cloud: CloudConfig, network: Network, sshKeys: Set<SshKey>): ResourceGroup {
        val resourceGroup = provisioner.createResourceGroup("vaultInfrastructure")
        val vault1Volume = Volume("vault-1_${location}", location)
        resourceGroup.addResource(vault1Volume)
        val vault1FloatingIp = FloatingIp("vault-1", location, mapOf("role" to "vault", "name" to "vault-1"))

        val vault1Record = DnsRecord("vault-1.${environment.name}", vault1FloatingIp, dnsZone = rootZone)
        resourceGroup.addResource(vault1Record)

        val variables = HashMap<String, IResourceLookup<String>>()
        variables.putAll(defaultCloudInitVariables(cloud, environment, rootZone, vault1FloatingIp))
        variables["hostname"] = ConstantDataSource("vault-1")
        variables["ssh_identity_ed25519_key"] = Base64Encode(ConstantDataSource(environment.sshConfig.sshIdentityPrivateKey))
        variables["ssh_identity_ed25519_pub"] = Base64Encode(ConstantDataSource(environment.sshConfig.sshIdentityPublicKey))
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
                volume = vault1Volume
        )
        resourceGroup.addResource(vault1Server)

        val vaultRecord = object : DnsRecord("vault.${environment.name}", floatingIp = vault1FloatingIp, dnsZone = rootZone, server = vault1Server) {
            override fun getHealthCheck(): () -> Boolean {
                return {
                    val url = "https://${name}.${dnsZone.name()}:8200"
                    try {
                        URL(url).readText()
                        logger.info { "url '${url}' is healthy" }
                        true
                    } catch (e: Exception) {
                        logger.warn { "url '${url}' is unhealthy" }
                        false
                    }
                }
            }
        }
        resourceGroup.addResource(vaultRecord)


        val floatingIpAssignment = FloatingIpAssignment(server = vault1Server, floatingIp = vault1FloatingIp)
        resourceGroup.addResource(floatingIpAssignment)

        return resourceGroup
    }

    private fun defaultCloudInitVariables(cloud: CloudConfig, environment: CloudEnvironmentConfig, rootZone: DnsZone, floatingIp: FloatingIp): Map<out String, IResourceLookup<String>> {
        return mapOf(
                "solidblocks_cloud" to ConstantDataSource(cloud.name),
                "solidblocks_version" to ConstantDataSource(solidblocksVersion()),
                "solidblocks_root_domain" to ResourceLookup<DnsZoneRuntime>(rootZone) {
                    it.name
                },
                "solidblocks_environment" to ConstantDataSource(environment.name),
                "solidblocks_public_ip" to ResourceLookup<FloatingIpRuntime>(floatingIp) {
                    it.ipv4
                }
        )
    }

    private fun addBackupResourceGroup(location: String, environment: CloudEnvironmentConfig, rootZone: DnsZone, cloud: CloudConfig, network: Network, sshKeys: Set<SshKey>) {
        val resourceGroup = provisioner.createResourceGroup("backupInfrastructure")

        val backupVolume = Volume("backup_${location}", location)
        resourceGroup.addResource(backupVolume)

        val floatingIp = FloatingIp("backup", location, mapOf("role" to "backup", "name" to "backup"))
        resourceGroup.addResource(floatingIp)

        val dnsRecord = DnsRecord("backup.${environment.name}", floatingIp, dnsZone = rootZone)
        resourceGroup.addResource(dnsRecord)

        val variables = HashMap<String, IResourceLookup<String>>()
        variables.putAll(defaultCloudInitVariables(cloud, environment, rootZone, floatingIp))

        variables["ssh_identity_ed25519_key"] = Base64Encode(ConstantDataSource(environment.sshConfig.sshIdentityPrivateKey))
        variables["ssh_identity_ed25519_pub"] = Base64Encode(ConstantDataSource(environment.sshConfig.sshIdentityPublicKey))
        variables["storage_local_device"] = ResourceLookup<VolumeRuntime>(backupVolume) {
            it.device
        }

        variables["vault_addr"] = ConstantDataSource("https://vault.${environment.name}.${cloud.rootDomain}:8200")
        variables["vault_token"] = CustomDataSource {
            val vaultClient =
                    provisioner.provider(VaultTemplate::class.java).createClient()
            val result = vaultClient.opsForToken().create(
                    VaultTokenRequest.builder()
                            .displayName("backup")
                            .noParent(true)
                            .renewable(true)
                            .policies(listOf(BACKUP_POLICY_NAME))
                            .build()
            )
            result.token.token
        }


        val userData = UserDataDataSource("lib-cloud-init-generated/backup-cloud-init.sh", variables)
        val server = Server(
                "backup",
                network,
                userData,
                sshKeys = sshKeys,
                location = location,
                volume = backupVolume,
                dependencies = listOf(dnsRecord)
        )
        resourceGroup.addResource(server)
        resourceGroup.addResource(FloatingIpAssignment(server = server, floatingIp = floatingIp))
    }

    private fun createVaultConfigResourceGroup(parentResourceGroup: ResourceGroup,
                                               cloud: CloudConfig,
                                               environment: CloudEnvironmentConfig) {

        val resourceGroup = provisioner.createResourceGroup("vaultConfig", setOf(parentResourceGroup))

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
        resourceGroup.addResource(hostPkiBackendRole)

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
        resourceGroup.addResource(hostSshBackendRole)

        val userSshMount = VaultMount(userSshMountName(cloud, environment),
                "ssh")
        val userSshBackendRole = VaultSshBackendRole(
                name = userSshMountName(cloud, environment),
                keyType = "ca",
                maxTtl = "168h",
                ttl = "168h",
                allowHostCertificates = false,
                allowUserCertificates = true,
                mount = userSshMount
        )
        resourceGroup.addResource(userSshBackendRole)

        val kvMount = VaultMount(kvMountName(cloud, environment), "kv-v2")
        resourceGroup.addResource(kvMount)

        //JacksonUtils.toMap()
        val solidblocksConfig =
                VaultKV("solidblocks/cloud/config",
                        mapOf(), kvMount)
        resourceGroup.addResource(solidblocksConfig)

        val hetznerConfig =
                VaultKV("solidblocks/cloud/providers/hetzner",
                        mapOf(HETZNER_CLOUD_API_TOKEN_RO_KEY to environment.configValues.getConfigValue(HETZNER_CLOUD_API_TOKEN_RO_KEY)!!.value), kvMount)
        resourceGroup.addResource(hetznerConfig)

        val githubConfig =
                VaultKV("solidblocks/cloud/providers/github",
                        mapOf(GITHUB_TOKEN_RO_KEY to environment.configValues.getConfigValue(GITHUB_TOKEN_RO_KEY)!!.value), kvMount)
        resourceGroup.addResource(githubConfig)

        val controllerPolicy = VaultPolicy(
                CONTROLLER_POLICY_NAME,
                setOf(

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/config").capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/providers/github")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/providers/hetzner")
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
        resourceGroup.addResource(controllerPolicy)

        val backupPolicy = VaultPolicy(
                BACKUP_POLICY_NAME,
                setOf(

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/config")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/providers/github")
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path(
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/providers/hetzner")
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
        resourceGroup.addResource(backupPolicy)
    }


}
