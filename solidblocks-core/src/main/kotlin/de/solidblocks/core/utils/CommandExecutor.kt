package de.solidblocks.core.utils

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

interface CommandExecutor {

    class CommandResult(
        val success: Boolean,
        val stdout: List<String> = ArrayList(),
        val stderr: List<String> = ArrayList()
    ) {

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
        environment: Map<String, String>,
        vararg command: String
    ): CommandResult

    companion object {
        fun createReader(inputStream: InputStream, target: MutableList<String>, printStream: Boolean): Thread {
            return Thread {
                try {
                    val inputStreamReader = InputStreamReader(inputStream).use {
                        val bufferedReader = BufferedReader(it).use { reader ->
                            val iterator = reader.lineSequence().iterator()
                            while (iterator.hasNext()) {
                                val line = iterator.next()
                                if (printStream) {
                                    println(line)
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
