package de.solidblocks.vault

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.vault.VaultConstants.kvMountName
import mu.KotlinLogging

data class VaultWriteRequest(val data: Map<*, *>)

class VaultManager(address: String, val _token: String, private val reference: EnvironmentReference) : BaseVaultManager(address, _token) {

    private val logger = KotlinLogging.logger {}

    val objectMapper = jacksonObjectMapper()

    private fun kvPath(path: String) = "/${kvMountName(reference)}/data/$path"

    fun storeKv(path: String, data: Any): Boolean {
        vaultTemplate.write(kvPath(path), VaultWriteRequest(objectMapper.convertValue(data, Map::class.java)))
        return true
    }

    fun <T> loadKv(path: String, clazz: Class<T>): T? {
        val response = vaultTemplate.read(kvPath(path))

        if (response.data == null) {
            logger.error { "no data returned for '${kvPath(path)}'" }
            return null
        }

        return jacksonObjectMapper().convertValue(response.data!!["data"] as Map<*, *>, clazz)
    }

    fun hasKv(path: String): Boolean {
        return vaultTemplate.read(kvPath(path)) != null
    }
}
