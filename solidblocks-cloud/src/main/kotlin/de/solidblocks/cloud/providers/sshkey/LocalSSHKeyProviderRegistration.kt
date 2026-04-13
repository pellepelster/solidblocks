package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderRegistration

val SSH_KEY_TYPE = "ssh_key"

class LocalSSHKeyProviderRegistration :
    SSHKeyProviderRegistration<
        LocalSSHKeyProviderConfiguration,
        LocalSSHKeyProviderConfigurationRuntime,
        LocalSSHKeyProviderManager,
        > {
    override val supportedConfiguration = LocalSSHKeyProviderConfiguration::class
    override val supportedRuntime = LocalSSHKeyProviderConfigurationRuntime::class

    override fun createManager() = LocalSSHKeyProviderManager()

    override fun createFactory() = LocalSSHKeyProviderConfigurationFactory()

    override val type = SSH_KEY_TYPE
}
