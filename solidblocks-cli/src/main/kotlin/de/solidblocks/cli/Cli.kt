package de.solidblocks.cli

import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.cloud.commands.*
import de.solidblocks.cli.cloud.commands.config.CloudConfigCommand
import de.solidblocks.cli.cloud.commands.config.CloudCreateCommand
import de.solidblocks.cli.cloud.commands.config.CloudDeleteCommand
import de.solidblocks.cli.cloud.commands.config.CloudEnvironmentCreateCommand
import de.solidblocks.cli.cloud.commands.config.CloudListCommand
import de.solidblocks.cli.cloud.commands.config.CloudRotateCommand
import de.solidblocks.cli.cloud.commands.config.CloudSshConfigCommand
import de.solidblocks.cli.self.SolidBlocksCli

fun main(args: Array<String>) {
    System.getProperties().setProperty("org.jooq.no-logo", "true")

    SolidBlocksCli()
        .subcommands(
                CloudCommand().subcommands(
                        CloudConfigCommand().subcommands(
                                CloudListCommand(),
                                CloudRotateCommand(),
                                CloudDeleteCommand(),
                                CloudSshConfigCommand(),
                                CloudCreateCommand(),
                                CloudEnvironmentCreateCommand()
                        ),
                        CloudTestCommand(), CloudDestroyCommand(), CloudBootstrapCommand(), TenantBootstrapCommand()
                )
        )
        .main(args)
}
