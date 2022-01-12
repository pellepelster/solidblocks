package de.solidblocks.vault

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.vault.VaultConstants.ENVIRONMENT_TOKEN_TTL
import mu.KotlinLogging
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultTokenRequest

data class VaultWriteRequest(val data: Map<*, *>)

abstract class BaseVaultManager<REFERENCE : EnvironmentReference>(vaultTemplate: VaultTemplate, val reference: REFERENCE) :
    BaseVaultAdminManager(vaultTemplate) {

    private val logger = KotlinLogging.logger {}

    private val objectMapper = jacksonObjectMapper()

    abstract fun kvPath(path: String): String

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

    fun createEnvironmentToken(name: String, policy: String): String {
        val result = vaultTemplate.opsForToken().create(
            VaultTokenRequest.builder().displayName(name).noParent(true).renewable(true).ttl(ENVIRONMENT_TOKEN_TTL)
                .policies(listOf(policy)).build()
        )
        return result.token.token
    }
}
