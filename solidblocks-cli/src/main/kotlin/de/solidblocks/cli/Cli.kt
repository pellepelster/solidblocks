package de.solidblocks.cli

import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.cloud.CloudCommand
import de.solidblocks.cli.commands.cloud.CloudCreateCommand
import de.solidblocks.cli.commands.environments.EnvironmentBootstrapCommand
import de.solidblocks.cli.commands.environments.EnvironmentCommand
import de.solidblocks.cli.commands.environments.EnvironmentConfigCommand
import de.solidblocks.cli.commands.environments.EnvironmentCreateCommand
import de.solidblocks.cli.commands.environments.EnvironmentDestroyCommand
import de.solidblocks.cli.commands.environments.EnvironmentRotateSecretsCommand
import de.solidblocks.cli.commands.environments.EnvironmentSshConfigCommand
import de.solidblocks.cli.commands.tenants.TenantBootstrapCommand
import de.solidblocks.cli.commands.tenants.TenantCommand
import de.solidblocks.cli.commands.tenants.TenantCreateCommand
import de.solidblocks.cli.self.SolidBlocksCli

fun main(args: Array<String>) {
    System.getProperties().setProperty("org.jooq.no-logo", "true")

    SolidBlocksCli()
        .subcommands(
            CloudCommand().subcommands(
                EnvironmentConfigCommand(), CloudCreateCommand()
            ),

            EnvironmentCommand().subcommands(
                EnvironmentCreateCommand(),
                EnvironmentConfigCommand(),
                EnvironmentBootstrapCommand(),
                EnvironmentDestroyCommand(),
                EnvironmentRotateSecretsCommand(),
                EnvironmentSshConfigCommand()
            ),

            TenantCommand().subcommands(
                TenantCreateCommand(),
                TenantBootstrapCommand()
            )
        )
        .main(args)
}
