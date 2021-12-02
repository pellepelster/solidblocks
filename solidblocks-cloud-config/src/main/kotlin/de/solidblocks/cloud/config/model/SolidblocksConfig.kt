package de.solidblocks.cloud.config.model

import com.fasterxml.jackson.annotation.JsonProperty

const val CONFIG_API_KEY = "api_key"
const val CONFIG_BACKUP_PASSWORD_KEY = "backup_password"

data class SolidblocksConfig(

    @JsonProperty("docker_registry")
    val dockerRegistry: DockerRegistryConfig? = null,

    @JsonProperty(CONFIG_API_KEY)
    val apiKey: String,

    @JsonProperty(CONFIG_BACKUP_PASSWORD_KEY)
    val backupPassword: String,

)
