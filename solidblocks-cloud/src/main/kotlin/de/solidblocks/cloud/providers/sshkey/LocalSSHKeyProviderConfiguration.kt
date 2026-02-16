package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.providers.ssh.SSHKeyProviderConfiguration

class LocalSSHKeyProviderConfiguration(override val name: String, val privateKey: String?) :
    SSHKeyProviderConfiguration {
  override val type = "ssh_keys"
}
