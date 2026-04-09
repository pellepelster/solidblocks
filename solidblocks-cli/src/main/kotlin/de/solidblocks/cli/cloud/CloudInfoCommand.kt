package de.solidblocks.cli.cloud

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import de.solidblocks.cli.cloud.CloudHelpCommand.Companion.printMarkdown
import de.solidblocks.cloud.CloudManager
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.services.CloudInfo
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.logError
import kotlinx.serialization.json.Json

class CloudInfoCommand : CliktCommand(name = "info") {
    override fun help(context: Context) = "show details for a cloud configuration"

    private val configFile by argument().file(mustExist = true)

    enum class Format {
        text,
        json,
    }

    val format by option().choice("text", "json").enum<Format>(true).default(Format.text)

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

        when (format) {
            Format.text -> {
                when (val result = manager.info(runtime)) {
                    is Error<String> -> {
                        logError(result.error)
                        throw ProgramResult(1)
                    }

                    is Success<String> -> printMarkdown(result)
                }
            }

            Format.json -> {
                when (val result = manager.infoJson(runtime)) {
                    is Error<CloudInfo> -> {
                        logError(result.error)
                        throw ProgramResult(1)
                    }

                    is Success<CloudInfo> -> println(Json.encodeToString(result.data))
                }
            }
        }
    }
}
