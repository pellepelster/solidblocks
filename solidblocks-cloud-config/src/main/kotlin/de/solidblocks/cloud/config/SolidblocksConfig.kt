package de.solidblocks.cloud.config

import com.fasterxml.jackson.annotation.JsonProperty

const val CONFIG_CONSUL_MASTER_TOKEN_KEY = "consul_master_token"
const val CONFIG_CONSUL_SECRET_KEY = "consul_secret"
const val CONFIG_ROOT_USERNAME_KEY = "root_username"
const val CONFIG_ROOT_PASSWORD_KEY = "root_password"
const val CONFIG_API_KEY = "api_key"
const val CONFIG_SMTP_KEY = "smtp"
const val CONFIG_BACKUP_PASSWORD_KEY = "backup_password"
const val CONFIG_ADMIN_EMAIL_KEY = "admin_email"
const val CONFIG_ADMIN_PASSWORD_KEY = "admin_password"
const val CONFIG_DOMAIN_KEY = "domain"

data class SolidblocksConfig(

    @JsonProperty(CONFIG_CONSUL_MASTER_TOKEN_KEY)
    val consulMasterToken: String,

    @JsonProperty(CONFIG_CONSUL_SECRET_KEY)
    var consulSecret: String,

    @JsonProperty("docker_registry")
    val dockerRegistry: DockerRegistryConfig,

    @JsonProperty(CONFIG_ROOT_USERNAME_KEY)
    val rootUsername: String = "root",

    @JsonProperty(CONFIG_ROOT_PASSWORD_KEY)
    val rootPassword: String,

    @JsonProperty(CONFIG_API_KEY)
    val apiKey: String,

    @JsonProperty(CONFIG_DOMAIN_KEY)
    val domain: String,

    @JsonProperty(CONFIG_SMTP_KEY)
    val smtp: SmtpConfig? = null,

    @JsonProperty(CONFIG_BACKUP_PASSWORD_KEY)
    val backupPassword: String,

    @JsonProperty(CONFIG_ADMIN_EMAIL_KEY)
    val adminEmail: String,

    @JsonProperty(CONFIG_ADMIN_PASSWORD_KEY)
    val adminPassword: String
)
