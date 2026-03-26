package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderConfigurtionRuntime
import java.nio.file.Path
import java.security.KeyPair

interface SSHKeyProviderRuntime : ProviderConfigurtionRuntime {
  val keyPair: KeyPair
  val privateKey: Path
}

fun List<ProviderConfigurtionRuntime>.sshKeyProvider() =
    this.filterIsInstance<SSHKeyProviderRuntime>().single()
