package de.solidblocks.vault

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.ModelConstants.serviceId
import de.solidblocks.cloud.model.ModelConstants.vaultTokenName
import de.solidblocks.vault.VaultConstants.ENVIRONMENT_TOKEN_TTL
import de.solidblocks.vault.VaultConstants.SERVICE_BASE_POLICY_NAME
import de.solidblocks.vault.VaultConstants.SERVICE_TOKEN_TTL
import de.solidblocks.vault.VaultConstants.kvMountName
import mu.KotlinLogging
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultTokenRequest

data class VaultWriteRequest(val data: Map<*, *>)

class VaultManager(vaultTemplate: VaultTemplate, private val reference: EnvironmentReference) :
    BaseVaultManager(vaultTemplate) {

    constructor(address: String, token: String, reference: EnvironmentReference) : this(
        createVaultTemplate(
            address,
            token
        ),
        reference
    )

    private val logger = KotlinLogging.logger {}

    private val objectMapper = jacksonObjectMapper()

    private fun kvPath(path: String) = "/${kvMountName(reference)}/data/$path"

    fun storeKv(path: String, data: Any): Boolean {
        vaultTemplate.write(kvPath(path), VaultWriteRequest(objectMapper.convertValue(data, Map::class.java)))
        return true
    }

    fun <T> loadKv(path: String, clazz: Class<T>): T? {
        val response = vaultTemplate.read(kvPath(path))

        if (response == null || response.data == null) {
            logger.error { "no data returned for '${kvPath(path)}'" }
            return null
        }

        return jacksonObjectMapper().convertValue(response.data!!["data"] as Map<*, *>, clazz)
    }

    fun hasKv(path: String): Boolean {
        return vaultTemplate.read(kvPath(path)) != null
    }

    fun createServiceToken(name: String, reference: ServiceReference): String {
        val result = vaultTemplate.opsForToken().create(
            VaultTokenRequest.builder()
                .displayName(vaultTokenName(name, reference))
                .noParent(true)
                .renewable(true)
                .ttl(SERVICE_TOKEN_TTL)
                .policies(listOf(SERVICE_BASE_POLICY_NAME, serviceId(reference)))
                .build()
        )
        return result.token.token
    }

    fun createEnvironmentToken(name: String, policy: String): String {
        val result = vaultTemplate.opsForToken().create(
            VaultTokenRequest.builder()
                .displayName(name)
                .noParent(true)
                .renewable(true)
                .ttl(ENVIRONMENT_TOKEN_TTL)
                .policies(listOf(policy))
                .build()
        )
        return result.token.token
    }
}
