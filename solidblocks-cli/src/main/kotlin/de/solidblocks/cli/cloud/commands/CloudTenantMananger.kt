package de.solidblocks.cli.cloud.commands

import de.solidblocks.api.resources.dns.DnsZone
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.TenantConfig
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.HetznerCloudCredentialsProvider
import de.solidblocks.provisioner.hetzner.cloud.getHetznerCloudApiToken
import de.solidblocks.provisioner.hetzner.dns.HetznerDnsCredentialsProvider
import de.solidblocks.provisioner.hetzner.dns.getHetznerDnsApiToken
import de.solidblocks.provisioner.vault.provider.VaultClientProvider
import mu.KotlinLogging
import org.apache.commons.net.util.SubnetUtils
import org.springframework.stereotype.Component

@Component
class CloudTenantMananger(
        val cloudCredentialsProvider: HetznerCloudCredentialsProvider,
        val dnsCredentialsProvider: HetznerDnsCredentialsProvider,
        val vaultClientProvider: VaultClientProvider,
        val provisioner: Provisioner,
        val cloudConfigurationManager: CloudConfigurationManager
) {
    private val logger = KotlinLogging.logger {}

    fun destroy(name: String): Boolean {
        val cloud = cloudConfigurationManager.getTenant(name)

        val hetznerCloudApiToken = cloud.configurations.getHetznerCloudApiToken()
        if (hetznerCloudApiToken == null) {
            logger.error { "hetzner cloud api token configuration not found" }
            return false
        }

        cloudCredentialsProvider.addApiToken(hetznerCloudApiToken.value)
        provisioner.destroyAll()
        return true
    }

    fun show(name: String): Boolean {
        val cloud = cloudConfigurationManager.getTenant(name)

        val hetznerCloudApiToken = cloud.configurations.getHetznerCloudApiToken()
        if (hetznerCloudApiToken == null) {
            logger.error { "hetzner cloud api token configuration not found" }
            return false
        }

        val hetznerDnsApiToken = cloud.configurations.getHetznerDnsApiToken()
        if (hetznerDnsApiToken == null) {
            logger.error { "hetzner dns api token configuration not found" }
            return false
        }

        this.cloudCredentialsProvider.addApiToken(hetznerCloudApiToken.value)
        this.dnsCredentialsProvider.addApiToken(hetznerDnsApiToken.value)

        provisioner.addProvider(vaultClientProvider)

        createTenantModel(cloud)

        provisioner.lookup()

        return true
    }

    fun bootstrap(name: String): Boolean {

        val cloud = cloudConfigurationManager.getTenant(name)

        val hetznerCloudApiToken = cloud.configurations.getHetznerCloudApiToken()
        if (hetznerCloudApiToken == null) {
            logger.error { "hetzner cloud api token configuration not found" }
            return false
        }

        val hetznerDnsApiToken = cloud.configurations.getHetznerDnsApiToken()
        if (hetznerDnsApiToken == null) {
            logger.error { "hetzner dns api token configuration not found" }
            return false
        }

        this.cloudCredentialsProvider.addApiToken(hetznerCloudApiToken.value)
        this.dnsCredentialsProvider.addApiToken(hetznerDnsApiToken.value)

        val sshKeys = listOf(SshKey(cloud.name, cloud.sshConfig.sshPublicKey))

        createTenantModel(cloud, sshKeys)

        return !provisioner.apply()
    }

    private fun createTenantModel(
            cloud: TenantConfig,
            sshKeys: List<SshKey> = emptyList()
    ) {
        val rootZone = DnsZone(cloud.solidblocksConfig.domain)

        val subnet = SubnetUtils("10.0.0.0/32").info
        val network = Network(cloud.name, subnet)
        val location = "nbg1"

        val baseLayer = provisioner.createResourceGroup("baseLayer")
        baseLayer.addResource(network)

        val vaultLayer = provisioner.createResourceGroup("vault")
        //vaultLayer.addResource(hetznerProviderConfig)
        /*
        val userSshMount = VaultMount(userSshMountName(cloud.name), "ssh")
        val userSshBackendRole = VaultSshBackendRole(
                name = userSshMountName(cloud.name),
                keyType = "ca",
                maxTtl = "24h",
                ttl = "24h",
                allowHostCertificates = false,
                allowUserCertificates = true,
                allowedUsers = "*",
                defaultExtensions = VaultSshBackendRoleDefaultExtensions("", ""),
                mount = userSshMount
        )
        vaultLayer.addResource(userSshBackendRole)


        /*
        val vault1Volume = Volume("vault-1", location)
        val vault1FloatingIp = FloatingIp("vault-1", location, mapOf("role" to "vault", "name" to "vault-1"))
        val vault1Record = DnsRecord("vault-1.${cloud.name}", vault1FloatingIp, zone = rootZone)

        val baseLayer = provisioner.createLayer("baseLayer")
        baseLayer.addResource(vault1Record)

        val seedRecord = DnsRecord("seed.${cloud.name}", vault1FloatingIp, zone = rootZone)
        baseLayer.addResource(seedRecord)

        val vaultRecord = DnsRecord("vault.${cloud.name}", vault1FloatingIp, zone = rootZone)
        baseLayer.addResource(vaultRecord)

        val variables = HashMap<String, IDataSource<String>>()
        variables["cloud_name"] = ConstantDataSource(cloud.name)
        variables["docker_registry_ro_username"] = ConstantDataSource(cloud.seedConfig.roUsername)
        variables["docker_registry_ro_password"] = ConstantDataSource(cloud.seedConfig.roPassword)
        variables["docker_registry_rw_username"] = ConstantDataSource(cloud.seedConfig.rwUsername)
        variables["docker_registry_rw_password"] = ConstantDataSource(cloud.seedConfig.rwPassword)
        variables["vault_hostname"] = ConstantDataSource("vault-1")
        variables["ssh_identity_ed25519_key"] = Base64Encode(ConstantDataSource(cloud.sshConfig.sshIdentityPrivateKey))
        variables["ssh_identity_ed25519_pub"] = Base64Encode(ConstantDataSource(cloud.sshConfig.sshIdentityPublicKey))
        variables["storage_local_device"] = ResourceLookup<VolumeRuntime>(vault1Volume) {
            it.device
        }
        variables["floating_ip"] = ResourceLookup<FloatingIpRuntime>(vault1FloatingIp) {
            it.ipv4
        }
        variables["root_domain"] = ResourceLookup<DnsZoneRuntime>(rootZone) {
            it.name
        }

        val userData = UserDataDataSource("cloud-init-vault.sh", variables)
        val vault1Server = Server(
            "vault-1",
            network,
            userData,
            sshKeys = sshKeys,
            location = location,
            volume = vault1Volume,
            dependencies = listOf(seedRecord, vault1Record)
        )
        baseLayer.addResource(vault1Server)

        val floatingIpAssignment = FloatingIpAssignment(vault1Server, vault1FloatingIp)
        baseLayer.addResource(floatingIpAssignment)

        addVaultLayer(cloud)

        val controllerLayer = provisioner.createLayer("controller")
        val controller0Volume = Volume("controller-0", location)

        val controllerVariables = HashMap<String, IDataSource<String>>()
        controllerVariables["vault_token"] = CustomDataSource {
            val vaultClient = provisioner.provider(VaultTemplate::class.java).createClient()
            val result = vaultClient.opsForToken().create(
                VaultTokenRequest.builder()
                    .displayName("controller-0")
                    .noParent(true)
                    .renewable(true)
                    .policies(listOf(CONTROLLER_POLICY_NAME))
                    .build()
            )
            result.token.token
        }
        controllerVariables["solidblocks_instance_id"] = ConstantDataSource(UUID.randomUUID().toString())
        controllerVariables["solidblocks_version"] = ConstantDataSource(solidblocksVersion())
        controllerVariables["solidblocks_development_mode"] = ConstantDataSource("true")
        controllerVariables["solidblocks_debug_level"] = ConstantDataSource("0")
        controllerVariables["root_domain"] = ConstantDataSource(cloud.solidblocksConfig.domain)
        controllerVariables["cloud_name"] = ConstantDataSource(cloud.name)
        controllerVariables["controller_node_count"] = ConstantDataSource("1")
        controllerVariables["storage_local_device"] = ResourceLookup<VolumeRuntime>(controller0Volume) {
            it.device
        }

        val controllerFloatingIp =
            FloatingIp("controller-0", location, mapOf("role" to "controller", "name" to "controller-0"))
        controllerLayer.addResource(controllerFloatingIp)

        val controllerUserData = UserDataDataSource("cloud-init-controller.sh", controllerVariables)
        val controller0 = Server(
            "controller-0",
            network,
            controllerUserData,
            sshKeys = sshKeys,
            location = location,
            volume = controller0Volume,
            labels = mapOf("name" to "controller-0", "role" to "controller")
        )
        controllerLayer.addResource(controller0)

        val controller0Record =
            DnsRecord("controller-0.${cloud.name}", floatingIp = controllerFloatingIp, zone = rootZone)
        controllerLayer.addResource(controller0Record)

        val controller0PrivateRecord =
            DnsRecord("controller-0.private.${cloud.name}", server = controller0, zone = rootZone)
        controllerLayer.addResource(controller0PrivateRecord)


        val controllerFloatingIpAssignment = FloatingIpAssignment(controller0, controllerFloatingIp)
        controllerLayer.addResource(controllerFloatingIpAssignment)

        val backupVolume = Volume("backup", location)

        val backupVariables = HashMap<String, IDataSource<String>>()
        backupVariables["vault_token"] = CustomDataSource {
            val vaultClient = provisioner.provider(VaultTemplate::class.java).createClient()
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
        backupVariables["solidblocks_instance_id"] = ConstantDataSource(UUID.randomUUID().toString())
        backupVariables["solidblocks_version"] = ConstantDataSource(solidblocksVersion())
        backupVariables["solidblocks_development_mode"] = ConstantDataSource("true")
        backupVariables["solidblocks_debug_level"] = ConstantDataSource("0")
        backupVariables["root_domain"] = ConstantDataSource(cloud.solidblocksConfig.domain)
        backupVariables["cloud_name"] = ConstantDataSource(cloud.name)
        backupVariables["storage_local_device"] = ResourceLookup<VolumeRuntime>(backupVolume) {
            it.device
        }


        val backupUserData = UserDataDataSource("cloud-init-backup.sh", backupVariables)
        val backupServer = Server(
            "backup",
            network,
            backupUserData,
            sshKeys = sshKeys,
            location = location,
            volume = backupVolume,
            labels = mapOf("name" to "backup", "role" to "backup")
        )
        controllerLayer.addResource(backupServer)

         */
    }

    private fun addVaultLayer(cloud: CloudConfig) {
        val vaultLayer = provisioner.createLayer("vaultLayer")
        provisioner.addProvider(
            VaultRootClientProvider(
                cloud.name,
                "https://vault-1.${cloud.name}.${cloud.solidblocksConfig.domain}:8200",
                cloudConfigurationManager
            )
        )

        val hostPkiMount = VaultMount(pkiMountName(cloud.name), "pki")
        val hostPkiBackendRole = VaultPkiBackendRole(
            name = pkiMountName(cloud.name),
            allowAnyName = true,
            generateLease = true,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = hostPkiMount
        )
        vaultLayer.addResource(hostPkiBackendRole)

        val hostSshMount = VaultMount(hostSshMountName(cloud.name), "ssh")
        val hostSshBackendRole = VaultSshBackendRole(
            name = hostSshMountName(cloud.name),
            keyType = "ca",
            maxTtl = "168h",
            ttl = "168h",
            allowHostCertificates = true,
            allowUserCertificates = false,
            mount = hostSshMount
        )
        vaultLayer.addResource(hostSshBackendRole)

        val kvMount = VaultMount(kvMountName(cloud.name), "kv-v2")
        vaultLayer.addResource(kvMount)

        val solidblocksConfig =
            VaultKV("solidblocks/cloud/config", JacksonUtils.toMap(cloud.solidblocksConfig), kvMount)
        vaultLayer.addResource(solidblocksConfig)

        val hetznerProviderConfig = VaultKV(
            "solidblocks/providers/hetzner",
            JacksonUtils.toMap(HetznerProviderConfig(cloud.configurations.getHetznerCloudApiToken()?.value!!)),
            kvMount
        )

        val controllerPolicy = VaultPolicy(
            CONTROLLER_POLICY_NAME,
            setOf(
                Policy.Rule.builder().path("${kvMountName(cloud.name)}/data/solidblocks/cloud/config")
                    .capabilities(Policy.BuiltinCapabilities.READ)
                    .build(),
                Policy.Rule.builder().path("${kvMountName(cloud.name)}/data/solidblocks/providers/hetzner")
                    .capabilities(Policy.BuiltinCapabilities.READ)
                    .build(),
                Policy.Rule.builder().path("${pkiMountName(cloud.name)}/issue/${pkiMountName(cloud.name)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE)
                    .build(),
                Policy.Rule.builder().path("${userSshMountName(cloud.name)}/sign/${userSshMountName(cloud.name)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE)
                    .build(),
                Policy.Rule.builder().path("${userSshMountName(cloud.name)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ)
                    .build(),
                Policy.Rule.builder().path("${hostSshMountName(cloud.name)}/sign/${hostSshMountName(cloud.name)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE)
                    .build(),
                Policy.Rule.builder().path("${hostSshMountName(cloud.name)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ)
                    .build(),
            ),
        )
        vaultLayer.addResource(controllerPolicy)

        val backupPolicy = VaultPolicy(
            BACKUP_POLICY_NAME,
            setOf(
                Policy.Rule.builder().path("${kvMountName(cloud.name)}/data/solidblocks/cloud/config")
                    .capabilities(Policy.BuiltinCapabilities.READ)
                    .build(),
                Policy.Rule.builder().path("${pkiMountName(cloud.name)}/issue/${pkiMountName(cloud.name)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE)
                    .build(),
                Policy.Rule.builder().path("${userSshMountName(cloud.name)}/sign/${userSshMountName(cloud.name)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE)
                    .build(),
                Policy.Rule.builder().path("${userSshMountName(cloud.name)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ)
                    .build(),
                Policy.Rule.builder().path("${hostSshMountName(cloud.name)}/sign/${hostSshMountName(cloud.name)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE)
                    .build(),
                Policy.Rule.builder().path("${hostSshMountName(cloud.name)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ)
                    .build(),

                ),
        )
        vaultLayer.addResource(backupPolicy)
         */
    }

}
