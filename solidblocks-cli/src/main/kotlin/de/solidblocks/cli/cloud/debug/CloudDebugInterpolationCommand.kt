package de.solidblocks.cli.cloud.debug

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError

class CloudDebugInterpolationCommand : CliktCommand(name = "interpolation") {
    override fun help(context: Context) = "test and verify string substitution"

    val interpolated by argument(help = "Interpolated string to test")

    val configFile by option(help = "Solidblocks cloud config file").file(mustExist = true, canBeFile = true, canBeDir = false).required()

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

        when (val result = manager.debugInterpolation(runtime, interpolated)) {
            is Error<Unit> -> {
                logError(result.error)
                throw ProgramResult(1)
            }

            is Success<Unit> -> {
            }
        }
    }
}
