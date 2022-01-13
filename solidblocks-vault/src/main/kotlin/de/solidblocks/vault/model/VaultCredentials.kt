package de.solidblocks.vault.model

data class VaultCredentials(val rootToken: String, val unsealKeys: List<String>)
