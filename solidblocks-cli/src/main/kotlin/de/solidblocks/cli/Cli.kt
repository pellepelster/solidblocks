package de.solidblocks.cli

import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.cloud.CloudCommand
import de.solidblocks.cli.commands.cloud.CloudCreateCommand
import de.solidblocks.cli.commands.environment.*
import de.solidblocks.cli.commands.service.ServiceBootstrapCommand
import de.solidblocks.cli.commands.service.ServiceCommand
import de.solidblocks.cli.commands.service.ServiceCreateCommand
import de.solidblocks.cli.commands.tenant.TenantBootstrapCommand
import de.solidblocks.cli.commands.tenant.TenantCommand
import de.solidblocks.cli.commands.tenant.TenantCreateCommand
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
                EnvironmentSshConfigCommand(),
                EnvironmentAgentsCommand()
            ),

            TenantCommand().subcommands(
                TenantCreateCommand(),
                TenantBootstrapCommand()
            ),

            ServiceCommand().subcommands(
                ServiceCreateCommand(),
                ServiceBootstrapCommand()
            )
        )
        .main(args)
}
