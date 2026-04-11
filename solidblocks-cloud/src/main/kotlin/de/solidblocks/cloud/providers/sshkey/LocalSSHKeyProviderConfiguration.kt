package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderConfiguration

class LocalSSHKeyProviderConfiguration(override val name: String, val privateKey: String?) : SSHKeyProviderConfiguration {
    override val type = SSH_KEY_TYPE
}
