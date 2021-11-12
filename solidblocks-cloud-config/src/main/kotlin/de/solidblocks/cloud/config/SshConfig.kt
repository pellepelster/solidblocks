package de.solidblocks.cloud.config

data class SshConfig(
    val sshIdentityPrivateKey: String,
    val sshIdentityPublicKey: String,
    val sshPrivateKey: String,
    val sshPublicKey: String,
) {
    companion object {
        const val CONFIG_SSH_PUBLIC_KEY = "ssh_public_key"
        const val CONFIG_SSH_PRIVATE_KEY = "ssh_private_key"

        const val CONFIG_SSH_IDENTITY_PUBLIC_KEY = "ssh_identity_public_key"
        const val CONFIG_SSH_IDENTITY_PRIVATE_KEY = "ssh_identity_private_key"
    }
}
