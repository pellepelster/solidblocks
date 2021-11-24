package de.solidblocks.cloud.config

data class ConsulSecrets(
    val consul_secret: String,
    val consul_master_token: String
)
