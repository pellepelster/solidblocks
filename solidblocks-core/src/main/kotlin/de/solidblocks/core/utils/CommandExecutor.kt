package de.solidblocks.core.utils

import mu.KotlinLogging
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration

interface CommandExecutor {

    class CommandResult(
        val code: Int,
        val stdout: List<String> = ArrayList(),
        val stderr: List<String> = ArrayList()
    ) {

        val success: Boolean
            get() = code == 0

        fun stdoutAsString(): String? {
            return stdout.joinToString("\n")
        }

        fun stderrAsString(): String? {
            return stderr.joinToString("\n")
        }
    }

    fun executeCommand(vararg command: String): CommandResult

    fun executeCommand(
        workingDir: File?,
        printStream: Boolean = false,
        environment: Map<String, String>,
        timeout: Duration = Duration.ofMinutes(1),
        vararg command: String
    ): CommandResult

    companion object {

        private val logger = KotlinLogging.logger {}

        fun createReader(prefix: String, inputStream: InputStream, target: MutableList<String>, printStream: Boolean): Thread {
            return Thread {
                try {
                    InputStreamReader(inputStream).use {
                        BufferedReader(it).use { reader ->
                            val iterator = reader.lineSequence().iterator()
                            while (iterator.hasNext()) {
                                val line = iterator.next()
                                if (printStream) {
                                    logger.info { "$prefix -> $line" }
                                }

                                target.add(line)
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException()
                }
            }
        }
    }
}
