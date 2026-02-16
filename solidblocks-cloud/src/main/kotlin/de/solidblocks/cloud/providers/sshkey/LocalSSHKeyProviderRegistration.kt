package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.providers.ssh.SSHKeyProviderRegistration

class LocalSSHKeyProviderRegistration :
    SSHKeyProviderRegistration<
        LocalSSHKeyProviderConfiguration,
        LocalSSHKeyProviderRuntime,
        LocalSSHKeyProviderManager,
    > {
  override val supportedConfiguration = LocalSSHKeyProviderConfiguration::class
  override val supportedRuntime = LocalSSHKeyProviderRuntime::class

  override fun createConfigurationManager() = LocalSSHKeyProviderManager()

  override fun createConfigurationFactory() = LocalSSHKeyProviderConfigurationFactory()

  override val type = "ssh_key"
}
