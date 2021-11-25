package de.solidblocks.cli.self

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.cli.config.CliApplication
import de.solidblocks.cli.self.SolidBlocksCli.Companion.DB_PASSWORD_KEY
import de.solidblocks.cli.self.SolidBlocksCli.Companion.DB_PATH_KEY
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext

abstract class BaseSpringCommand(
    help: String = "",
    name: String? = null,
    val cliClass: Class<*> = CliApplication::class.java
) :
    CliktCommand(name = name, help = help) {

    override fun run() {
        val config = currentContext.findOrSetObject { mutableMapOf<String, String>() }
        val dbPassword = config[DB_PASSWORD_KEY]
        val dbPath = config[DB_PATH_KEY]

        val args = mapOf("db.path" to dbPath, "db.password" to dbPassword) + extraArgs()
        val argumentList = args.map { "--${it.key}=${it.value}" }

        run(SpringApplication.run(cliClass, *argumentList.toTypedArray()))
    }

    open fun extraArgs() = emptyMap<String, String>()

    abstract fun run(applicationContext: ApplicationContext)

}

