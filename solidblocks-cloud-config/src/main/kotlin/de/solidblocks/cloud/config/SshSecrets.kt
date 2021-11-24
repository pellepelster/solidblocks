package de.solidblocks.cloud.config

data class SshSecrets(
    val sshIdentityPrivateKey: String,
    val sshIdentityPublicKey: String,
    val sshPrivateKey: String,
    val sshPublicKey: String,
)
