package de.solidblocks.cli.self

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import mu.KotlinLogging
import kotlin.io.path.Path
import kotlin.system.exitProcess

class SolidBlocksCli : CliktCommand() {

    private val logger = KotlinLogging.logger {}

    companion object {
        const val DB_PASSWORD_KEY = "dbPassword"
        const val DB_PATH_KEY = "dbPath"
    }
    val dbPassword: String by option(help = "secret for the solidblocks db").required()

    val dbPath: String by option(help = "path for the solidblocks db").required()

    override fun run() {

        if (!Path(dbPath).isAbsolute) {
            logger.error { "please provide an absolute path for solidblocks db" }
            exitProcess(1)
        }

        val config = currentContext.findOrSetObject { mutableMapOf<String, String>() }
        config[DB_PASSWORD_KEY] = dbPassword
        config[DB_PATH_KEY] = dbPath
    }
}
