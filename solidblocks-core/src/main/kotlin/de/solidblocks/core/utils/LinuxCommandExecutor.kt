package de.solidblocks.core.utils

import mu.KotlinLogging
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class LinuxCommandExecutor : CommandExecutor {

    private val logger = KotlinLogging.logger {}

    override fun executeCommand(vararg command: String): CommandExecutor.CommandResult {
        return executeCommand(null, mapOf(), *command)
    }

    fun executeCommand(environment: Map<String, String>, vararg command: String): CommandExecutor.CommandResult {
        return executeCommand(null, environment, *command)
    }

    override fun executeCommand(
        workingDir: File?,
        environment: Map<String, String>,
        vararg command: String
    ): CommandExecutor.CommandResult {
        var stdoutThread: Thread? = null
        var stderrThread: Thread? = null

        try {
            val processBuilder = ProcessBuilder(*command)
            if (workingDir != null) {
                processBuilder.directory(workingDir)
            }
            processBuilder.environment().putAll(environment)
            val process = processBuilder.start()
            val stdout = ArrayList<String>()
            val stderr = ArrayList<String>()
            stdoutThread = CommandExecutor.createReader(process.inputStream, stdout, false)
            stderrThread = CommandExecutor.createReader(process.errorStream, stderr, false)

            stdoutThread.start()
            stderrThread.start()

            process.waitFor(180, TimeUnit.SECONDS)

            return CommandExecutor.CommandResult(success = process.exitValue() == 0, stderr = stderr, stdout = stdout)
        } catch (e: Exception) {
            logger.error { "execution of command '${command.joinToString { " " }}' failed" }
            throw RuntimeException(e)
        } finally {
            stderrThread?.join(5000)
            stdoutThread?.join(5000)
        }
    }

    companion object {

        fun toString(inputStream: InputStream): String {
            return try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.lines().collect(Collectors.joining(System.lineSeparator()))
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        fun executeBackgroundCommand(command: Array<String>, stringBuffer: MutableList<String>) {
            try {
                val pb = ProcessBuilder(*command)
                val process = pb.start()
                val thread = CommandExecutor.createReader(process.inputStream, stringBuffer, true)
                thread.start()
            } catch (e: Exception) {
                throw RuntimeException(e)
            } finally {
                // killswitch.set(true);
            }
        }
    }
}
