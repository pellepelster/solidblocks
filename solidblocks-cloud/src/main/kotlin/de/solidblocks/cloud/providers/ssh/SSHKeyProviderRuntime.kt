package de.solidblocks.cloud.providers.ssh

import de.solidblocks.cloud.providers.ProviderRuntime
import java.nio.file.Path
import java.security.KeyPair

interface SSHKeyProviderRuntime : ProviderRuntime {
    val keyPair: KeyPair
    val privateKey: Path

    companion object {
        val PRIVATE_KEY_PATH_PLACEHOLDER: String = "<private_key_path>"
    }
}