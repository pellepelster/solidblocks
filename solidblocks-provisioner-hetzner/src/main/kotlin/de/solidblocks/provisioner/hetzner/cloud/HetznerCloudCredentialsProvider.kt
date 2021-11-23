package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.cloud.config.CloudConfigValue
import org.springframework.stereotype.Component

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
