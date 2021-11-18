package de.solidblocks.cli.self

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.CliApplication
import de.solidblocks.cli.config.GlobalConfig
import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import kotlin.io.path.Path
import kotlin.system.exitProcess

class SolidBlocksCli : CliktCommand() {

    private val logger = KotlinLogging.logger {}

    val dbPassword: String by option(help = "secret for the solidblocks db").required()

    val dbPath: String by option(help = "path for the solidblocks db").required()

    override fun run() {

        if (!Path(dbPath).isAbsolute) {
            logger.error { "please provide an absolute path for solidblocks db" }
            exitProcess(1)
        }

        SpringApplication.run(CliApplication::class.java, "--db.path=$dbPath", "--db.password=$dbPassword")
    }
}
