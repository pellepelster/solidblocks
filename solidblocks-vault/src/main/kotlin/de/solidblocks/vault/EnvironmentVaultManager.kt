package de.solidblocks.vault

import de.solidblocks.base.BaseConstants.vaultTokenName
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.ServiceReference
import de.solidblocks.vault.VaultConstants.servicePolicyName
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultTokenRequest

class EnvironmentVaultManager(vaultTemplate: VaultTemplate, val reference: EnvironmentReference) :
    BaseVaultManager(vaultTemplate) {

    constructor(address: String, token: String, reference: EnvironmentReference) : this(
        createVaultTemplate(
            address, token
        ),
        reference
    )

    fun createServiceToken(name: String, reference: ServiceReference): String {
        val result = vaultTemplate.opsForToken().create(
            VaultTokenRequest.builder().displayName(vaultTokenName(name, reference)).noParent(true)
                .renewable(true)
                .ttl(VaultConstants.SERVICE_TOKEN_TTL).policies(
                    listOf(
                        servicePolicyName(reference)
                    )
                ).build()
        )
        return result.token.token
    }

    fun createEnvironmentToken(name: String, policy: String): String {
        val result = vaultTemplate.opsForToken().create(
            VaultTokenRequest.builder().displayName(name).noParent(true).renewable(true)
                .ttl(VaultConstants.ENVIRONMENT_TOKEN_TTL)
                .policies(listOf(policy)).build()
        )
        return result.token.token
    }

    override fun kvPath(path: String) = "/${VaultConstants.kvMountName(reference)}/data/$path"
}
