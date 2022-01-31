package de.solidblocks.cli

import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.api.ApiCommand
import de.solidblocks.cli.commands.cloud.CloudCommand
import de.solidblocks.cli.commands.cloud.CloudCreateCommand
import de.solidblocks.cli.commands.environment.*
import de.solidblocks.cli.commands.environment.agents.AgentsListCommand
import de.solidblocks.cli.commands.environment.agents.AgentsUpdateCommand
import de.solidblocks.cli.commands.service.ServiceBootstrapCommand
import de.solidblocks.cli.commands.service.ServiceCommand
import de.solidblocks.cli.commands.service.ServiceCreateCommand
import de.solidblocks.cli.commands.tenant.TenantBootstrapCommand
import de.solidblocks.cli.commands.tenant.TenantCommand
import de.solidblocks.cli.commands.tenant.TenantCreateCommand
import de.solidblocks.cli.commands.tenant.TenantDestroyCommand
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
                EnvironmentApplyCommand(),
                EnvironmentDestroyCommand(),
                EnvironmentStatusCommand(),
                EnvironmentRotateSecretsCommand(),
                EnvironmentSshConfigCommand(),
                EnvironmentAgentsCommand().subcommands(AgentsUpdateCommand(), AgentsListCommand())
            ),

            TenantCommand().subcommands(
                TenantCreateCommand(),
                TenantBootstrapCommand(),
                TenantDestroyCommand()
            ),

            ServiceCommand().subcommands(
                ServiceCreateCommand(),
                ServiceBootstrapCommand()
            ),
            ApiCommand()
        )
        .main(args)
}
