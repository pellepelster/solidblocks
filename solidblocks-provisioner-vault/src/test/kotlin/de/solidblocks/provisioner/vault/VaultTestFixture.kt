package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import org.testcontainers.containers.DockerComposeContainer
import java.util.*

class VaultTestFixture(
    environment: DockerComposeContainer<*>,
    cloudConfigurationManager: CloudConfigurationManager,
    provisioner: Provisioner
) {

    var cloudName: String = UUID.randomUUID().toString()

    init {
        val cloudName = UUID.randomUUID().toString()
        val environmentName = UUID.randomUUID().toString()

        cloudConfigurationManager.create(cloudName, "domain1", "email1", emptyList())

        provisioner.addProvider(
                VaultRootClientProvider(
                        cloudName,
                        environmentName,
                        "http://localhost:${environment.getServicePort("vault1", 8200)}",
                        cloudConfigurationManager
                )
        )
    }
}
