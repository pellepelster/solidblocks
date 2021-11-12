package de.solidblocks.cloud.config

import com.fasterxml.jackson.annotation.JsonProperty

data class SmtpConfig(
    @JsonProperty("smtp_username")
    val smtpUsername: String? = null,

    @JsonProperty("smtp_password")
    val smtpPassword: String? = null,

    @JsonProperty("smtp_host")
    val smtpHost: String = "localhost",

    @JsonProperty("smtp_port")
    private val smtpPort: Int = 25,

    @JsonProperty("smtp_auth_enabled")
    val smtpAuthEnabled: Boolean = false,

    @JsonProperty("smtp_tls_enabled")
    val smtpTlsEnabled: Boolean = false

)
