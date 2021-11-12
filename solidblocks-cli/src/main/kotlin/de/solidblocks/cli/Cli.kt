package de.solidblocks.cli

import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.cloud.commands.CloudBootstrapCommand
import de.solidblocks.cli.cloud.commands.CloudCommand
import de.solidblocks.cli.cloud.commands.CloudDestroyCommand
import de.solidblocks.cli.cloud.commands.CloudTestCommand
import de.solidblocks.cli.cloud.commands.config.CloudConfigCommand
import de.solidblocks.cli.cloud.commands.config.CloudCreateCommand
import de.solidblocks.cli.cloud.commands.config.CloudDeleteCommand
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
                                CloudCreateCommand()
                        ),
                        CloudTestCommand(), CloudDestroyCommand(), CloudBootstrapCommand()
                )
        )
        .main(args)
}
