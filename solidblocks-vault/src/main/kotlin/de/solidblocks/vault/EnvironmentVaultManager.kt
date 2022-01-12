package de.solidblocks.vault

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.ModelConstants
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultTokenRequest

class EnvironmentVaultManager(vaultTemplate: VaultTemplate, reference: EnvironmentReference) :
    BaseVaultManager<EnvironmentReference>(vaultTemplate, reference) {

    constructor(address: String, token: String, reference: EnvironmentReference) : this(
        createVaultTemplate(
            address, token
        ),
        reference
    )

    fun createServiceToken(name: String, reference: ServiceReference): String {
        val result = vaultTemplate.opsForToken().create(
            VaultTokenRequest.builder().displayName(ModelConstants.vaultTokenName(name, reference)).noParent(true)
                .renewable(true)
                .ttl(VaultConstants.SERVICE_TOKEN_TTL).policies(
                    listOf(
                        VaultConstants.environentBasePolicyName(reference),
                        VaultConstants.tenantBaseServicePolicyName(reference),
                        VaultConstants.servicePolicyName(reference)
                    )
                ).build()
        )
        return result.token.token
    }

    override fun kvPath(path: String) = "/${VaultConstants.kvMountName(reference)}/data/$path"
}
