package de.solidblocks.cli.self

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.CliApplication
import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.stereotype.Component
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.system.exitProcess

@Component
class InitCommand :
    CliktCommand(name = "init", help = "initialize solidblocks2") {

    private val logger = KotlinLogging.logger {}

    val dbPassword: String by option(help = "secret for the solidblocks2 db").required()

    val dbPath: String by option(help = "path for the solidblocks2 db").required()

    @OptIn(ExperimentalPathApi::class)
    override fun run() {

        if (!Path(dbPath).isAbsolute) {
            logger.error { "please provide an absolute path for solidblocks2 db" }
            exitProcess(1)
        }

        SpringApplication.run(CliApplication::class.java, "--db.path=$dbPath", "--db.password=$dbPassword")
    }
}
