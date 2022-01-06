package de.solidblocks.cloud

import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.ModelConstants.serviceId
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.vault.VaultConstants.kvMountName
import de.solidblocks.vault.VaultConstants.pkiMountName
import mu.KotlinLogging
import org.springframework.vault.support.Policy.BuiltinCapabilities.*
import org.springframework.vault.support.Policy.Rule.builder

class ServiceProvisioner(val provisioner: Provisioner) {

    private val logger = KotlinLogging.logger {}

    fun createService(reference: ServiceReference) {
        val resourceGroup = provisioner.createResourceGroup(reference.service)

        val backupServicePolicy = VaultPolicy(
            serviceId(reference),
            setOf(
                builder().path(
                    "${kvMountName(reference.toEnvironment())}/data/solidblocks/cloud/providers/github"
                ).capabilities(READ).build(),

                builder().path("${pkiMountName(reference.toEnvironment())}/issue/${pkiMountName(reference.toEnvironment())}")
                    .capabilities(UPDATE).build(),

                builder().path("auth/token/renew-self").capabilities(UPDATE).build(),
                builder().path("/auth/token/lookup-self").capabilities(READ).build(),
            ),
        )
        resourceGroup.addResource(backupServicePolicy)
        provisioner.apply()
    }
}
