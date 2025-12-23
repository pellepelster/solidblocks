package de.solidblocks.infra.test.ssh

data class SshCommandResult(
    val success: Boolean,
    val stdOut: String? = null,
    val stdErr: String? = null,
    val error: Exception? = null
)
