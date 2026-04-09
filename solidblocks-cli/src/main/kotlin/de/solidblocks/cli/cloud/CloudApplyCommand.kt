package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import de.solidblocks.cli.cloud.CloudHelpCommand.Companion.printMarkdown
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudApplyCommand : CliktCommand(name = "apply") {
    override fun help(context: Context) = "roll out a cloud configuration"

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

        printMarkdown(manager.apply(runtime))
    }
}
