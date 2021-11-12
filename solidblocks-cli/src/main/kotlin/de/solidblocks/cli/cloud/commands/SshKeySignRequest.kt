package de.solidblocks.cli.cloud.commands

data class SshKeySignRequest(val public_key: String, val valid_principals: String)
