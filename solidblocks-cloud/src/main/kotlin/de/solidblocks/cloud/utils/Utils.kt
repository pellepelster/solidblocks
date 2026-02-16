package de.solidblocks.cloud.utils

import java.io.InputStreamReader

fun commandExists(command: String) =
    try {
      val isWindows = System.getProperty("os.name").lowercase().contains("win")
      val checkCommand =
          if (isWindows) {
            listOf("cmd.exe", "/c", "where", command)
          } else {
            listOf("sh", "-c", "command -v $command")
          }

      val process =
          ProcessBuilder(checkCommand)
              .redirectOutput(ProcessBuilder.Redirect.PIPE)
              .redirectError(ProcessBuilder.Redirect.PIPE)
              .start()

      process.waitFor() == 0
    } catch (e: Exception) {
      false
    }

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)

fun runCommand(command: List<String>, stdin: String? = null) =
    try {
      val isWindows = System.getProperty("os.name").lowercase().contains("win")

      val process = ProcessBuilder(command).start()

      if (stdin != null) {
        process.outputStream.use { output ->
          output.write(stdin.toByteArray())
          output.flush()
          output.close()
        }
      }

      val exitCode = process.waitFor()

      CommandResult(
          exitCode,
          InputStreamReader(process.inputStream).readText(),
          InputStreamReader(process.errorStream).readText(),
      )
    } catch (e: Exception) {
      null
    }

public fun List<String>.indentWithYamlObjectMarker() =
    this.withIndex().map {
      if (it.index == 0) {
        "- ${it.value}"
      } else {
        "  ${it.value}"
      }
    }
