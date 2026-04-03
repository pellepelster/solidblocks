package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.markdown.Markdown
import de.solidblocks.cli.utils.createTerminal
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.services.ServiceInfo
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudInfoCommand : CliktCommand(name = "info") {

  override fun help(context: Context) = "show details for a cloud configuration"

  private val configFile by argument().file(mustExist = true)

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

    when (val result = manager.info(runtime)) {
      is Error<List<ServiceInfo>> -> {
        logError(result.error)
        throw ProgramResult(1)
      }

      is Success<List<ServiceInfo>> -> {
        val terminal = createTerminal()

        val services =
            result.data.joinToString("\n") {
              """
## ${it.serviceName}
### Linked Environment Variables

Environment Variables that will be injected when this service is linked to another service
"""
                  .trimMargin()
            }

        terminal.println(
            Markdown(
                """
# Services
$services
                        """
                    .trimIndent(),
                true,
                false,
            ),
        )
      }
    }
  }
}
