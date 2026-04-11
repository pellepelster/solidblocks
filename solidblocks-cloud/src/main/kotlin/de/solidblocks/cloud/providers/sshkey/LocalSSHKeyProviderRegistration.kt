package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.providers.ssh.SSHKeyProviderRegistration

val SSH_KEY_TYPE = "ssh_key"

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

    override val type = SSH_KEY_TYPE
}
