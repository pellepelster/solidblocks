package de.solidblocks.cloud

import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ModelConstants.serviceId
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.vault.VaultConstants
import de.solidblocks.vault.VaultConstants.kvMountName
import de.solidblocks.vault.VaultConstants.pkiMountName
import mu.KotlinLogging
import org.springframework.vault.support.Policy

class ServiceProvisioner(val provisioner: Provisioner) {

    private val logger = KotlinLogging.logger {}

    fun createService(reference: ServiceReference) {
        val resourceGroup = provisioner.createResourceGroup(reference.service)

        val backupServicePolicy = VaultPolicy(
                serviceId(reference),
                setOf(
                        Policy.Rule.builder().path(
                                "${kvMountName(reference.toEnvironment())}/data/solidblocks/cloud/providers/github"
                        ).capabilities(Policy.BuiltinCapabilities.READ).build(),

                        Policy.Rule.builder().path("${pkiMountName(reference.toEnvironment())}/issue/${pkiMountName(reference.toEnvironment())}")
                                .capabilities(Policy.BuiltinCapabilities.UPDATE).build(),
                ),
        )
        resourceGroup.addResource(backupServicePolicy)
        provisioner.apply()
    }
}
