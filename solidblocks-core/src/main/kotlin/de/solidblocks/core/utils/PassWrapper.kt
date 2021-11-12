package de.solidblocks.core.utils

class PassWrapper(val secretsDir: String) {

    val commandExecutor = LinuxCommandExecutor()

    fun getSecret(key: String): String? {
        val result = commandExecutor.executeCommand(mapOf("PASSWORD_STORE_DIR" to secretsDir), "pass", key)

        if (!result.success) {
            throw RuntimeException(result.stderrAsString())
        }
        return result.stdoutAsString()
    }
}
