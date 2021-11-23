package de.solidblocks.provisioner.hetzner.dns

import de.solidblocks.cloud.config.CloudConfigValue
import org.springframework.stereotype.Component

@Component
class HetznerDnsCredentialsProvider {

    private val apiTokens = ArrayList<String>()

    fun addApiToken(apiToken: String) {
        this.apiTokens.add(apiToken)
    }

    fun defaultApiToken(): String {
        return apiTokens.first()
    }
}
