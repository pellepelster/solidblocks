package de.solidblocks.cloud.config

import com.fasterxml.jackson.annotation.JsonProperty

const val CONFIG_CONSUL_MASTER_TOKEN_KEY = "consul_master_token"
const val CONFIG_CONSUL_SECRET_KEY = "consul_secret"
const val CONFIG_API_KEY = "api_key"
const val CONFIG_BACKUP_PASSWORD_KEY = "backup_password"

data class SolidblocksConfig(

    @JsonProperty(CONFIG_CONSUL_MASTER_TOKEN_KEY)
    val consulMasterToken: String,

    @JsonProperty(CONFIG_CONSUL_SECRET_KEY)
    var consulSecret: String,

    @JsonProperty("docker_registry")
    val dockerRegistry: DockerRegistryConfig? = null,

    @JsonProperty(CONFIG_API_KEY)
    val apiKey: String,

    @JsonProperty(CONFIG_BACKUP_PASSWORD_KEY)
    val backupPassword: String,

)
