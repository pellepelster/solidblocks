package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.cloud.config.CloudConfigValue
import org.springframework.stereotype.Component

private const val CONFIG_API_TOKEN_KEY = "hetzner_cloud_api_key"

fun createHetznerCloudApiToken(apiToken: String): CloudConfigValue {
    return CloudConfigValue(CONFIG_API_TOKEN_KEY, apiToken)
}

fun List<CloudConfigValue>.getHetznerCloudApiToken(): CloudConfigValue? {
    return this.firstOrNull { it.name == CONFIG_API_TOKEN_KEY }
}

@Component
class HetznerCloudCredentialsProvider {

    private val apiTokens = ArrayList<String>()

    fun addApiToken(apiToken: String) {
        this.apiTokens.add(apiToken)
    }

    fun defaultApiToken(): String {
        return apiTokens.first()
    }
}
