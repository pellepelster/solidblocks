package de.solidblocks.base

import mu.KotlinLogging
import java.io.File
import java.util.concurrent.TimeUnit

data class CommandResult(val stdoutRaw: String, val stderrRaw: String, val exitCode: Int) {

    val stdout get() = stdoutRaw.trim()
    val stderr get() = stderrRaw.trim()
    val error get() = exitCode > 0
}

class CommandExecutor {

    private val logger = KotlinLogging.logger {}

    fun run(
        workingDir: File? = null,
        environment: Map<String, String> = emptyMap(),
        command: List<String>
    ): CommandResult {

        val proc = ProcessBuilder(*command.toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
        proc.environment().putAll(environment)

        if (workingDir != null) {
            proc.directory(workingDir)
        }

        val process = proc.start()

        process.waitFor(5, TimeUnit.MINUTES)

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        return CommandResult(stdout, stderr, process.exitValue())
    }
}
