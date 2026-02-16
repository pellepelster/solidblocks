package de.solidblocks.cloud.providers.sshkey

import de.solidblocks.cloud.providers.ssh.SSHKeyProviderRuntime
import java.security.KeyPair

data class LocalSSHKeyProviderRuntime(override val keyPair: KeyPair) : SSHKeyProviderRuntime
