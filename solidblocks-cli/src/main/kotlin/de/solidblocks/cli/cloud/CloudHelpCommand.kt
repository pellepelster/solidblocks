package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.api.InfrastructureResourceHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudHelpCommand : CliktCommand(name = "help") {

  private val configFile by argument().file().optional()

  init {
    installMordantMarkdown()
  }

  override fun help(context: Context) = "Solidblocks cloud configuration file documentation"

  // + DocumentationGenerator(CloudConfigurationFactory(emptyList(),
  // emptyList())).generateMarkdown()

  override fun run() {
    if (configFile != null) {
      val manager = CloudManager(configFile!!)
      val runtime =
          when (val result = manager.validate()) {
            is Error<CloudManager.CloudRuntime> -> {
              logError(result.error)
              throw ProgramResult(1)
            }

            is Success<CloudManager.CloudRuntime> -> result.data
          }

      // TODO workaround for testing inside gradle
      val t = Terminal(AnsiLevel.TRUECOLOR)
      t.println()

      when (val result = manager.help(runtime)) {
        is Error<List<InfrastructureResourceHelp>> -> {
          logError(result.error)
          throw ProgramResult(1)
        }

        is Success<List<InfrastructureResourceHelp>> -> {
          result.data.forEach {
            t.println(bold(it.title))
            t.println()
            t.println(it.help)
            t.println()
            t.println()
          }
        }
      }
    } else {}
  }
}
