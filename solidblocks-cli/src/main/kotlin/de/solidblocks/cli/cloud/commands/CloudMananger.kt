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
import de.solidblocks.api.resources.infrastructure.network.Subnet
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.utils.Base64Encode
import de.solidblocks.api.resources.infrastructure.utils.ConstantDataSource
import de.solidblocks.api.resources.infrastructure.utils.CustomDataSource
import de.solidblocks.api.resources.infrastructure.utils.ResourceLookup
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.cli.Contants.hostSshMountName
import de.solidblocks.cli.Contants.kvMountName
import de.solidblocks.cli.Contants.pkiMountName
import de.solidblocks.cli.Contants.userSshMountName
import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.CONSUL_SECRET_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.GITHUB_TOKEN_RO_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.GITHUB_USERNAME_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.cloud.config.Constants.Vault.Companion.BACKUP_POLICY_NAME
import de.solidblocks.cloud.config.Constants.Vault.Companion.CONTROLLER_POLICY_NAME
import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.getConfigValue
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.ResourceGroup
import de.solidblocks.provisioner.vault.kv.VaultKV
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRole
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRole
import mu.KotlinLogging
import org.apache.commons.net.util.SubnetUtils
import org.springframework.stereotype.Component
import org.springframework.vault.support.Policy
import org.springframework.vault.support.VaultTokenRequest
import java.net.URL
import java.time.Duration

@Component
class CloudMananger(
    val vaultRootClientProvider: VaultRootClientProvider,
    val provisioner: Provisioner,
    val cloudConfigurationManager: CloudConfigurationManager,
    val configurationContext: CloudConfigurationContext
) {
    private val logger = KotlinLogging.logger {}


    fun destroy(destroyVolumes: Boolean): Boolean {
        provisioner.destroyAll(destroyVolumes)
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

        logger.info { "creating/updating environment '${cloudName}' for cloud '$environmentName'" }

        createCloudModel(cloud, environment, setOf(SshKey("${cloud.name}-${environment.name}", environment.sshSecrets.sshPublicKey)))

        return provisioner.apply()
    }

    private fun createCloudModel(
        cloud: CloudConfiguration,
        environment: CloudEnvironmentConfiguration,
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

        val vaultResourceGroup = provisioner.createResourceGroup("vault")
        val floatingIp = createDefaultServer(
            ServerSpec(
                name = "vault-0",
                location = location,
                network = network,
                role = "vault",
                sshKeys = sshKeys,
                dnsHealthCheckPort = 8200
            ), vaultResourceGroup, cloud, environment, rootZone
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
                        logger.info { "url '${url}' is healthy" }
                        return@health true
                    } catch (e: Exception) {
                        logger.warn { "url '${url}' is unhealthy" }
                        return@health false
                    }
                }
            }
        })

        createVaultConfigResourceGroup(vaultResourceGroup, cloud, environment)

        val controllerNodeCount = 1
        val controllerGroup = provisioner.createResourceGroup("controller")
        createDefaultServer(
            ServerSpec(
                name = "controller-0",
                location = location,
                network = network,
                role = "controller",
                sshKeys = sshKeys,
                vaultPolicyName = CONTROLLER_POLICY_NAME,
                dnsHealthCheckPort = 8500,
                extraVariables = mapOf("controller_node_count" to ConstantDataSource(controllerNodeCount.toString()))
            ), controllerGroup, cloud, environment, rootZone
        )

        val resourceGroup = provisioner.createResourceGroup("backup")
        createDefaultServer(
                ServerSpec(
                        name = "backup",
                        location = location,
                        network = network,
                        role = "backup",
                        sshKeys = sshKeys,
                        vaultPolicyName = BACKUP_POLICY_NAME,
                        extraVariables = mapOf("controller_node_count" to ConstantDataSource(controllerNodeCount.toString()))
                ), resourceGroup, cloud, environment, rootZone
        )

    }

    private fun defaultCloudInitVariables(
        name: String,
        cloud: CloudConfiguration,
        environment: CloudEnvironmentConfiguration,
        rootZone: DnsZone,
        floatingIp: FloatingIp
    ): Map<out String, IResourceLookup<String>> {
        return mapOf(
            "solidblocks_cloud" to ConstantDataSource(cloud.name),
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
        val role: String,
        val location: String,
        val vaultPolicyName: String? = null,
        val network: Network,
        val sshKeys: Set<SshKey>,
        val dnsHealthCheckPort: Int? = null,
        val extraVariables: Map<String, IResourceLookup<String>> = emptyMap()
    )

    private fun createDefaultServer(
        serverSpec: ServerSpec,
        resourceGroup: ResourceGroup,
        cloud: CloudConfiguration,
        environment: CloudEnvironmentConfiguration,
        rootZone: DnsZone
    ): FloatingIp {
        val volume = resourceGroup.addResource(
            Volume(
                "${cloud.name}-${environment.name}-${serverSpec.name}-${serverSpec.location}",
                serverSpec.location
            )
        )
        val floatingIp = resourceGroup.addResource(
            FloatingIp(
                "${cloud.name}-${environment.name}-${serverSpec.name}",
                serverSpec.location,
                mapOf("role" to serverSpec.role, "name" to serverSpec.name)
            )
        )

        val variables = HashMap<String, IResourceLookup<String>>()
        variables.putAll(defaultCloudInitVariables(serverSpec.name, cloud, environment, rootZone, floatingIp))
        variables["storage_local_device"] = ResourceLookup<VolumeRuntime>(volume) {
            it.device
        }
        variables.putAll(serverSpec.extraVariables)

        variables["vault_addr"] = ConstantDataSource("https://vault.${environment.name}.${cloud.rootDomain}:8200")
        //TODO create minimal token for bootstrapping and create new one on boot with more privileges?
        if (serverSpec.vaultPolicyName != null) {
            variables["vault_token"] = CustomDataSource {
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


        val userData = UserDataDataSource("lib-cloud-init-generated/${serverSpec.role}-cloud-init.sh", variables)
        val server = resourceGroup.addResource(
            Server(
                    id = "${cloud.name}-${environment.name}-${serverSpec.name}",
                    network = serverSpec.network,
                    userData = userData,
                    sshKeys = serverSpec.sshKeys,
                    location = serverSpec.location,
                    volume = volume,
                    labels = mapOf("role" to serverSpec.role)
            )
        )

        resourceGroup.addResource(
            DnsRecord(
                    id = "${cloud.name}-${environment.name}-${serverSpec.name}",
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
                        logger.info { "url '${url}' is healthy" }
                        return@health true
                    } catch (e: Exception) {
                        logger.warn { "url '${url}' is unhealthy" }
                        return@health false
                    }
                }
            }
        })

        resourceGroup.addResource(FloatingIpAssignment(server = server, floatingIp = floatingIp))

        return floatingIp
    }

    private fun createVaultConfigResourceGroup(parentResourceGroup: ResourceGroup,
                                               cloud: CloudConfiguration,
                                               environment: CloudEnvironmentConfiguration
    ) {

        val resourceGroup = provisioner.createResourceGroup("vaultConfig", setOf(parentResourceGroup))

        val hostPkiMount = VaultMount(pkiMountName(cloud, environment), "pki")
        val hostPkiBackendRole = VaultPkiBackendRole(
                id = pkiMountName(cloud, environment),
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
                id = hostSshMountName(cloud, environment),
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
                id = userSshMountName(cloud, environment),
                keyType = "ca",
                maxTtl = "168h",
                ttl = "168h",
                allowedUsers = listOf("root"),
                defaultUser = "root",
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
                VaultKV(
                    "solidblocks/cloud/providers/github",
                    mapOf(
                        GITHUB_TOKEN_RO_KEY to environment.configValues.getConfigValue(GITHUB_TOKEN_RO_KEY)!!.value,
                        GITHUB_USERNAME_KEY to "pellepelster"
                    ), kvMount
                )
        resourceGroup.addResource(githubConfig)

        val consulConfig =
                VaultKV(
                    "solidblocks/cloud/config/consul",
                    mapOf(
                        CONSUL_SECRET_KEY to environment.configValues.getConfigValue(CONSUL_SECRET_KEY)!!.value,
                        CONSUL_MASTER_TOKEN_KEY to environment.configValues.getConfigValue(CONSUL_MASTER_TOKEN_KEY)!!.value
                    ), kvMount
                )
        resourceGroup.addResource(consulConfig)

        val controllerPolicy = VaultPolicy(
                CONTROLLER_POLICY_NAME,
                setOf(
                    Policy.Rule.builder().path(
                        "${kvMountName(cloud, environment)}/data/solidblocks/cloud/config"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(cloud, environment)}/data/solidblocks/cloud/config/consul"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "/auth/token/renew-self"
                    )
                        .capabilities(Policy.BuiltinCapabilities.UPDATE)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(cloud, environment)}/data/solidblocks/cloud/providers/github"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(cloud, environment)}/data/solidblocks/cloud/providers/hetzner"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder()
                        .path("${pkiMountName(cloud, environment)}/issue/${pkiMountName(cloud, environment)}")
                        .capabilities(Policy.BuiltinCapabilities.UPDATE)
                        .build(),

                    Policy.Rule.builder()
                        .path("${userSshMountName(cloud, environment)}/sign/${userSshMountName(cloud, environment)}")
                        .capabilities(
                            Policy.BuiltinCapabilities.UPDATE,
                            Policy.BuiltinCapabilities.CREATE
                        )
                        .build(),

                    Policy.Rule.builder().path("${userSshMountName(cloud, environment)}/config/ca")
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder()
                        .path("${hostSshMountName(cloud, environment)}/sign/${hostSshMountName(cloud, environment)}")
                        .capabilities(
                            Policy.BuiltinCapabilities.UPDATE,
                            Policy.BuiltinCapabilities.CREATE
                        )
                        .build(),

                    Policy.Rule.builder().path("${hostSshMountName(cloud, environment)}/config/ca")
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    )
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
                                "${kvMountName(cloud, environment)}/data/solidblocks/cloud/config/consul"
                        )
                                .capabilities(Policy.BuiltinCapabilities.READ)
                                .build(),

                        Policy.Rule.builder().path(
                                "/auth/token/renew-self")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE)
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
