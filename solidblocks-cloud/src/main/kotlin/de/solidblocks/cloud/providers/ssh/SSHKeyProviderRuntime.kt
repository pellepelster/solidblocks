package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import java.nio.file.Path
import java.security.KeyPair

interface SSHKeyProviderRuntime : ProviderConfigurationRuntime {
  val keyPair: KeyPair
  val privateKey: Path
}

fun List<ProviderConfigurationRuntime>.sshKeyProvider() =
    this.filterIsInstance<SSHKeyProviderRuntime>().single()
