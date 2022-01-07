package de.solidblocks.core.utils

import mu.KotlinLogging
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class LinuxCommandExecutor : CommandExecutor {

    private val logger = KotlinLogging.logger {}

    var process: Process? = null

    override fun executeCommand(vararg command: String): CommandExecutor.CommandResult {
        return executeCommand(command = command)
    }

    fun executeCommand(environment: Map<String, String>, vararg command: String): CommandExecutor.CommandResult {
        return executeCommand(environment = environment, command = command)
    }

    override fun executeCommand(
        workingDir: File?,
        printStream: Boolean,
        environment: Map<String, String>,
        timeout: Duration,
        vararg command: String
    ): CommandExecutor.CommandResult {
        var stdoutThread: Thread? = null
        var stderrThread: Thread? = null

        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()

        try {
            val processBuilder = ProcessBuilder(*command)

            if (workingDir != null) {
                processBuilder.directory(workingDir)
            }

            processBuilder.environment().putAll(environment)
            process = processBuilder.start()

            stdoutThread = CommandExecutor.createReader(process!!.inputStream, stdout, printStream)
            stderrThread = CommandExecutor.createReader(process!!.errorStream, stderr, printStream)
            stderrThread.start()

            process!!.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)

            return CommandExecutor.CommandResult(success = process!!.exitValue() == 0, stderr = stderr, stdout = stdout)
        } catch (e: IllegalThreadStateException) {
            logger.error(e) { "command did not finish in time '${command.joinToString { " " }}'" }

            return CommandExecutor.CommandResult(success = false, stderr = stderr, stdout = stdout)
        } catch (e: Exception) {
            logger.error(e) { "execution of command '${command.joinToString { " " }}' failed" }
            throw RuntimeException(e)
        } finally {
            stderrThread?.join(5000)
            stdoutThread?.join(5000)
            process?.destroyForcibly()
        }
    }

    fun kill() {
        process?.destroyForcibly()
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
