package de.solidblocks.vault

data class VaultCredentials(val rootToken: String, val unsealKeys: List<String>)
