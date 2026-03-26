package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudSSHConfigCommand : CliktCommand(name = "ssh-config") {

  private val configFile by argument().file(mustExist = true)

  override fun help(context: Context) = "TODO"

  override fun run() {
    val manager = CloudManager(configFile)

    val runtime =
        when (val result = manager.validate()) {
          is Error<CloudConfigurationRuntime> -> {
            logError(result.error)
            throw ProgramResult(1)
          }

          is Success<CloudConfigurationRuntime> -> result.data
        }

    when (val result = manager.writeSshConfig(runtime)) {
      is Error<Unit> -> {
        logError(result.error)
        throw ProgramResult(1)
      }

      else -> {}
    }
  }
}
