package de.solidblocks.cloud

import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.base.lookups.Lookups
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.SolidblocksDatabase
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.consul.Consul
import de.solidblocks.provisioner.hetzner.Hetzner
import de.solidblocks.provisioner.vault.Vault
import de.solidblocks.vault.VaultRootClientProvider

class SolidblocksAppplicationContext(jdbcUrl: String, val vaultAddressOverride: String? = null) {

    private var vaultRootClientProvider: VaultRootClientProvider? = null

    val configurationManager: CloudConfigurationManager

    init {
        val database = SolidblocksDatabase(jdbcUrl)
        database.ensureDBSchema()
        configurationManager = CloudConfigurationManager(database.dsl)
    }

    fun vaultRootClientProvider(cloud: String, environment: String): VaultRootClientProvider {
        if (vaultRootClientProvider == null) {
            vaultRootClientProvider = VaultRootClientProvider(cloud, environment, configurationManager, vaultAddressOverride)
        }

        return vaultRootClientProvider!!
    }

    fun environmentConfiguration(cloud: String, environment: String) =
        configurationManager.environmentByName(cloud, environment)

    fun createProvisioner(cloud: String, environment: String): Provisioner {

        val provisionerRegistry = ProvisionerRegistry()
        val provisioner = Provisioner(provisionerRegistry)

        val environmentConfiguration = environmentConfiguration(cloud, environment)

        Hetzner.registerProvisioners(provisionerRegistry, environmentConfiguration, provisioner)
        Hetzner.registerLookups(provisionerRegistry, provisioner)
        Lookups.registerLookups(provisionerRegistry, provisioner)
        Vault.registerProvisioners(
            provisionerRegistry
        ) {
            vaultRootClientProvider(cloud, environment).createClient()
        }
        Consul.registerProvisioners(provisionerRegistry, Consul.consulClient(environmentConfiguration))

        return provisioner
    }
}
