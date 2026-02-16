package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudPlanCommand : CliktCommand(name = "plan") {

  private val configFile by argument().file()

  override fun help(context: Context) = "TODO"

  override fun run() {
    val manager = CloudManager(configFile)

    val runtime =
        when (val result = manager.validate()) {
          is Error<CloudManager.CloudRuntime> -> {
            logError(result.error)
            throw ProgramResult(1)
          }

          is Success<CloudManager.CloudRuntime> -> result.data
        }

    when (val result = manager.plan(runtime)) {
      is Error<Map<ResourceGroup, List<ResourceDiff>>> -> {
        logError(result.error)
        throw ProgramResult(1)
      }
      else -> {}
    }
  }
}
