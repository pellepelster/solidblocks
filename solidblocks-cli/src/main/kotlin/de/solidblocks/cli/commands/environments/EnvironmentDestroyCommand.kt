package de.solidblocks.cli.commands.environments

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.cli.commands.BaseCloudEnvironmentCommand
import de.solidblocks.cloud.CloudMananger
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.consul.Consul
import de.solidblocks.provisioner.hetzner.Hetzner
import de.solidblocks.provisioner.utils.Lookups
import de.solidblocks.provisioner.vault.Vault
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider

class EnvironmentDestroyCommand :
    BaseCloudEnvironmentCommand(name = "destroy", help = "destroy environment") {

    val destroyVolumes by option("--destroy-volumes").flag(default = false)

    override fun run() {

        val configurationManager = CloudConfigurationManager(solidblocksDatabase().dsl)
        val vaultRootClientProvider = VaultRootClientProvider(cloud, environment, configurationManager)

        val environmentConfiguration: CloudEnvironmentConfiguration =
            configurationManager.environmentByName(cloud, environment)

        val provisionerRegistry = ProvisionerRegistry()
        val provisioner = Provisioner(provisionerRegistry)

        Hetzner.registerProvisioners(provisionerRegistry, environmentConfiguration, provisioner)
        Lookups.registerLookups(provisionerRegistry, provisioner)
        Vault.registerProvisioners(
            provisionerRegistry,
            Vault.vaultTemplateProvider(environmentConfiguration, configurationManager)
        )
        Consul.registerProvisioners(provisionerRegistry, Consul.consulClient(environmentConfiguration))

        val cloudManager = CloudMananger(vaultRootClientProvider, provisioner, configurationManager)

        cloudManager.destroy(destroyVolumes)
    }
}
