package de.solidblocks.cloud.providers.types.ssh

import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import java.nio.file.Path
import java.security.KeyPair

interface SSHKeyProviderConfigurationRuntime : ProviderConfigurationRuntime {
    val keyPair: KeyPair
    val privateKey: Path
}

fun List<ProviderConfigurationRuntime>.sshKeyProvider() = this.filterIsInstance<SSHKeyProviderConfigurationRuntime>().single()
