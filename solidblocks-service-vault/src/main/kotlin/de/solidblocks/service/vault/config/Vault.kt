package de.solidblocks.service.vault.config

data class Vault(val listener: Tcp, val storage: RaftStorage, val cluster_addr: String, val api_addr: String, val ui: Boolean = true)
