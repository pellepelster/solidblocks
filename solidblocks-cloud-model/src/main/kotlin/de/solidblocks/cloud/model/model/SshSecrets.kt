package de.solidblocks.cloud.model.model

data class SshSecrets(
    val sshIdentityPrivateKey: String,
    val sshIdentityPublicKey: String,
    val sshPrivateKey: String,
    val sshPublicKey: String,
)
