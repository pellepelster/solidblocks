package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderConfigurationRuntime
import java.nio.file.Path
import java.security.KeyPair

data class LocalSSHKeyProviderConfigurationRuntime(override val keyPair: KeyPair, override val privateKey: Path) : SSHKeyProviderConfigurationRuntime
