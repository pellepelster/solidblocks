package de.solidblocks.cloud

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.lookups.Base64Encode
import de.solidblocks.base.lookups.ConstantDataSource
import de.solidblocks.base.lookups.CustomDataSource
import de.solidblocks.base.lookups.ResourceLookup
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.cloud.NetworkUtils.solidblocksNetwork
import de.solidblocks.cloud.NetworkUtils.subnetForNetwork
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.ConfigConstants.cloudId
import de.solidblocks.cloud.config.ConfigConstants.defaultLabels
import de.solidblocks.cloud.config.ConfigConstants.networkName
import de.solidblocks.cloud.config.Role
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.consul.kv.ConsulKv
import de.solidblocks.provisioner.consul.policy.ConsulPolicy
import de.solidblocks.provisioner.consul.policy.ConsulRuleBuilder
import de.solidblocks.provisioner.consul.policy.Privileges
import de.solidblocks.provisioner.consul.token.ConsulToken
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIp
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIpAssignment
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIpRuntime
import de.solidblocks.provisioner.hetzner.cloud.network.Network
import de.solidblocks.provisioner.hetzner.cloud.network.Subnet
import de.solidblocks.provisioner.hetzner.cloud.server.Server
import de.solidblocks.provisioner.hetzner.cloud.server.UserData
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKey
import de.solidblocks.provisioner.hetzner.cloud.volume.Volume
import de.solidblocks.provisioner.hetzner.cloud.volume.VolumeRuntime
import de.solidblocks.provisioner.hetzner.dns.record.DnsRecord
import de.solidblocks.provisioner.hetzner.dns.zone.DnsZone
import de.solidblocks.provisioner.hetzner.dns.zone.DnsZoneRuntime
import de.solidblocks.vault.VaultConstants.BACKUP_POLICY_NAME
import de.solidblocks.vault.VaultConstants.CONTROLLER_POLICY_NAME
import de.solidblocks.vault.VaultRootClientProvider
import mu.KotlinLogging
import org.springframework.vault.support.VaultTokenRequest
import java.net.URL
import java.time.Duration
import java.util.*

class CloudManager(
    private val vaultRootClientProvider: VaultRootClientProvider,
    private val provisioner: Provisioner,
    val cloudConfigurationManager: CloudConfigurationManager,
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(destroyVolumes: Boolean): Boolean {
        provisioner.destroyAll(destroyVolumes)
        return true
    }

    fun bootstrap(cloudName: String, environmentName: String): Boolean {
        val environment = cloudConfigurationManager.environmentByName(cloudName, environmentName)

        logger.info { "creating/updating environment '$cloudName' for cloud '$environmentName'" }

        createCloudModel(environment, setOf(SshKey("${environment.cloud.name}-${environment.name}", environment.sshSecrets.sshPublicKey)))

        return provisioner.apply()
    }

    private fun createCloudModel(
        environment: CloudEnvironmentConfiguration,
        sshKeys: Set<SshKey> = emptySet()
    ) {
        val rootZone = DnsZone(environment.cloud.rootDomain)

        // val rootZone = DnsZone(cloud.solidblocksConfig.domain)

        val networkResourceGroup = provisioner.createResourceGroup("network")

        val network = Network(networkName(environment), solidblocksNetwork())
        networkResourceGroup.addResource(network)

        val subnet = Subnet(subnetForNetwork(solidblocksNetwork()), network)
        networkResourceGroup.addResource(subnet)

        val location = "nbg1"

        val vaultResourceGroup = provisioner.createResourceGroup("vault")
        val floatingIp = createDefaultServer(
            ServerSpec(
                name = "vault-0",
                location = location,
                network = network,
                role = Role.vault,
                sshKeys = sshKeys,
                dnsHealthCheckPort = 8200
            ),
            vaultResourceGroup, environment, rootZone
        )

        vaultResourceGroup.addResource(object : DnsRecord(
            id = "vault.${environment.name}",
            floatingIp = floatingIp,
            dnsZone = rootZone
        ) {
                override fun getHealthCheck(): () -> Boolean {
                    return health@{
                        val url = "https://${this.id}.${dnsZone.id()}:8200"
                        try {
                            URL(url).readText()
                            logger.info { "url '$url' is healthy" }
                            return@health true
                        } catch (e: Exception) {
                            logger.warn { "url '$url' is unhealthy" }
                            return@health false
                        }
                    }
                }
            })

        provisioner.addResourceGroup(VaultConfig.createVaultConfig(setOf(vaultResourceGroup), environment))

        val controllerNodeCount = 1
        val controllerGroup = provisioner.createResourceGroup("controller")
        val controllerFLoatingIp = createDefaultServer(
            ServerSpec(
                name = "controller-0",
                location = location,
                network = network,
                role = Role.controller,
                sshKeys = sshKeys,
                vaultPolicyName = CONTROLLER_POLICY_NAME,
                staticVariables = mapOf("controller_node_count" to ConstantDataSource(controllerNodeCount.toString()))
            ),
            controllerGroup, environment, rootZone
        )

        val consulDns = object : DnsRecord(
            id = "consul.${environment.name}",
            floatingIp = controllerFLoatingIp,
            dnsZone = rootZone
        ) {
            override fun getHealthCheck(): () -> Boolean {
                return health@{
                    val url = "http://${this.id}.${dnsZone.id()}:8500"
                    try {
                        URL(url).readText()
                        logger.info { "url '$url' is healthy" }
                        return@health true
                    } catch (e: Exception) {
                        logger.warn { "url '$url' is unhealthy" }
                        return@health false
                    }
                }
            }
        }
        controllerGroup.addResource(consulDns)

        val controllerConfigGroup = provisioner.createResourceGroup("controllerConfig", dependsOn = setOf(controllerGroup))
        controllerConfigGroup.addResource(ConsulKv("solidblocks/clouds/${cloudId(environment)}", dependsOn = setOf(consulDns)))

        val backupResourceGroup = provisioner.createResourceGroup("backup", setOf(controllerConfigGroup))

        val acl = ConsulPolicy("backup", ConsulRuleBuilder().addKeyPrefix("prefix1", Privileges.write).asPolicy())
        backupResourceGroup.addResource(acl)

        val backupTokenId = UUID.randomUUID()
        val backupToken = ConsulToken(backupTokenId, "backup", setOf(acl))
        backupResourceGroup.addResource(backupToken)

        createDefaultServer(
            ServerSpec(
                name = "backup",
                location = location,
                network = network,
                role = Role.backup,
                sshKeys = sshKeys,
                vaultPolicyName = BACKUP_POLICY_NAME,
                staticVariables = mapOf("controller_node_count" to ConstantDataSource(controllerNodeCount.toString())),
            ),
            backupResourceGroup, environment, rootZone
        )
    }

    private fun defaultCloudInitVariables(
        name: String,
        environment: CloudEnvironmentConfiguration,
        rootZone: DnsZone,
        floatingIp: FloatingIp
    ): Map<out String, IResourceLookup<String>> {
        return mapOf(
            "solidblocks_cloud" to ConstantDataSource(environment.cloud.name),
            "solidblocks_hostname" to ConstantDataSource(name),
            "solidblocks_version" to ConstantDataSource(solidblocksVersion()),
            "solidblocks_root_domain" to ResourceLookup<DnsZoneRuntime>(rootZone) {
                it.name
            },
            "solidblocks_environment" to ConstantDataSource(environment.name),
            "solidblocks_public_ip" to ResourceLookup<FloatingIpRuntime>(floatingIp) {
                it.ipv4
            },
            "ssh_identity_ed25519_key" to Base64Encode(ConstantDataSource(environment.sshSecrets.sshIdentityPrivateKey)),
            "ssh_identity_ed25519_pub" to Base64Encode(ConstantDataSource(environment.sshSecrets.sshIdentityPublicKey))
        )
    }

    private data class ServerSpec(
        val name: String,
        val role: Role,
        val location: String,
        val vaultPolicyName: String? = null,
        val network: Network,
        val sshKeys: Set<SshKey>,
        val dnsHealthCheckPort: Int? = null,
        val staticVariables: Map<String, IResourceLookup<String>> = emptyMap(),
        val ephemeralVariables: Map<String, IResourceLookup<String>> = emptyMap()
    )

    private fun createDefaultServer(
        serverSpec: ServerSpec,
        resourceGroup: ResourceGroup,
        environment: CloudEnvironmentConfiguration,
        rootZone: DnsZone
    ): FloatingIp {
        val volume = resourceGroup.addResource(
            Volume(
                id = "${environment.cloud.name}-${environment.name}-${serverSpec.name}-${serverSpec.location}",
                location = serverSpec.location,
                labels = defaultLabels(environment, serverSpec.role)
            )
        )
        val floatingIp = resourceGroup.addResource(
            FloatingIp(
                id = "${environment.cloud.name}-${environment.name}-${serverSpec.name}",
                location = serverSpec.location,
                labels = defaultLabels(environment, serverSpec.role)
            )
        )

        val staticVariables = HashMap<String, IResourceLookup<String>>()
        val ephemeralVariables = HashMap<String, IResourceLookup<String>>()
        staticVariables.putAll(defaultCloudInitVariables(serverSpec.name, environment, rootZone, floatingIp))
        staticVariables["storage_local_device"] = ResourceLookup<VolumeRuntime>(volume) {
            it.device
        }
        staticVariables.putAll(serverSpec.staticVariables)
        ephemeralVariables.putAll(serverSpec.ephemeralVariables)

        staticVariables["vault_addr"] = ConstantDataSource("https://vault.${environment.name}.${environment.cloud.rootDomain}:8200")
        // TODO create minimal token for bootstrapping and create new one on boot with more privileges?
        if (serverSpec.vaultPolicyName != null) {
            ephemeralVariables["vault_token"] = CustomDataSource {
                val vaultClient =
                    vaultRootClientProvider.createClient()
                val result = vaultClient.opsForToken().create(
                    VaultTokenRequest.builder()
                        .displayName(serverSpec.name)
                        .noParent(true)
                        .renewable(true)
                        .ttl(Duration.ofHours(36))
                        .policies(listOf(serverSpec.vaultPolicyName))
                        .build()
                )
                result.token.token
            }
        }

        val userData = UserData("lib-cloud-init-generated/${serverSpec.role}-cloud-init.sh", staticVariables, ephemeralVariables)
        val server = resourceGroup.addResource(
            Server(
                id = "${environment.cloud.name}-${environment.name}-${serverSpec.name}",
                network = serverSpec.network,
                userData = userData,
                sshKeys = serverSpec.sshKeys,
                location = serverSpec.location,
                volume = volume,
                labels = defaultLabels(environment, serverSpec.role)
            )
        )

        resourceGroup.addResource(
            DnsRecord(
                id = "${environment.cloud.name}-${environment.name}-${serverSpec.name}",
                floatingIp = floatingIp,
                dnsZone = rootZone,
                server = server
            )
        )

        resourceGroup.addResource(object : DnsRecord(
            id = "${serverSpec.name}.${environment.name}",
            floatingIp = floatingIp,
            dnsZone = rootZone,
            server = server
        ) {
                override fun getHealthCheck(): () -> Boolean {
                    return health@{
                        if (serverSpec.dnsHealthCheckPort == null) {
                            return@health true
                        }

                        val url = "https://${this.id}.${dnsZone.id()}:${serverSpec.dnsHealthCheckPort}"
                        try {
                            URL(url).readText()
                            logger.info { "url '$url' is healthy" }
                            return@health true
                        } catch (e: Exception) {
                            logger.warn { "url '$url' is unhealthy" }
                            return@health false
                        }
                    }
                }
            })

        resourceGroup.addResource(FloatingIpAssignment(server = server, floatingIp = floatingIp))

        return floatingIp
    }
}
