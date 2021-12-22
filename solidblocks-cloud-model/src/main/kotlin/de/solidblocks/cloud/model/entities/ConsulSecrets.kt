package de.solidblocks.cloud.model.entities

data class ConsulSecrets(
    val consul_secret: String,
    val consul_master_token: String
)
