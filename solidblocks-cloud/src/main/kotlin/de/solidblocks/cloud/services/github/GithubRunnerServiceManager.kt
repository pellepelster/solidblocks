package de.solidblocks.cloud.services.github

import de.solidblocks.cloud.Constants
import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.defaultServiceSubnet
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.serverNamePrefix
import de.solidblocks.cloud.Constants.serverPrivateIp
import de.solidblocks.cloud.Constants.serviceLabels
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.github.GitHubApi
import de.solidblocks.cloud.github.resources.Runner
import de.solidblocks.cloud.providers.github.GitHubUrlRuntime
import de.solidblocks.cloud.providers.github.GitHubUrlRuntime.Organization
import de.solidblocks.cloud.providers.github.GitHubUrlRuntime.Repository
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.context.ValidationContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.toResult
import de.solidblocks.cloud.services.InstanceRuntime
import de.solidblocks.cloud.services.ServerInfo
import de.solidblocks.cloud.services.ServiceInfo
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.services.createDefaultSSHIdentity
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfiguration
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfigurationRuntime
import de.solidblocks.cloud.services.sshConnectCommand
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.VERY_LONG_WAIT
import de.solidblocks.cloud.utils.aggregateErrors
import de.solidblocks.cloud.utils.hasError
import de.solidblocks.cloud.utils.markdown
import de.solidblocks.cloud.utils.waitForCondition
import de.solidblocks.cloudinit.Distributor
import de.solidblocks.cloudinit.GithubRunnerUserData
import de.solidblocks.utils.LogContext
import kotlinx.coroutines.runBlocking

class GithubRunnerServiceManager : ServiceManager<GithubRunnerServiceConfiguration, GithubRunnerServiceConfigurationRuntime> {

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: SSHProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            (0..<runtime.scale).map {
                ServerInfo(sshConnectCommand(context, cloud, runtime, it))
            },
            emptyList(),
        ),
    )

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: SSHProvisionerContext): Result<String> = Success(
        markdown {
            h1("Service '${runtime.name}'")

            h2("Servers")
            (0..<runtime.scale).forEach {
                text("to access server **${serverName(cloud.environment, runtime.name, it)}** via SSH, run")
                listOf(codeBlock(sshConnectCommand(context, cloud, runtime, it)))
            }

            h2("Github Runners")
            (0..<runtime.scale).forEach {
                h3("${runtime.name}-$it")
                p("Registered for **${cloud.githubProviderRuntime().githubUrl.toUrl()}**")
                bold("Labels")
                list {
                    runtime.labels.forEach { item(it) }
                }
            }
        },
    )

    override fun status(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: SSHProvisionerContext): Result<String> {
        /*
        val sshClient = context.createOrGetSshClient(serverName(cloud.environment, runtime.name))
        val result = sshClient.command("systemctl is-active ${runtime.name}.service")
        text(if (result.exitCode == 0) "active" else "inactive (${result.stdOut.trim()})")
         */

        return markdown {
        }.let { Success(it) }
    }

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext): List<BaseInfrastructureResource<*>> {
        val gitHub = githubContext(cloud)

        val runnerToken = runBlocking {
            gitHub.createRegistrationToken()
        }

        val servers = (0..runtime.scale - 1).flatMap {
            val runnerName = "${runtime.name}-$it"
            val serverName = serverName(cloud.environment, runtime.name, it)
            val defaultResources = createDefaultSSHIdentity(cloud, runtime, it)

            val userData = UserData(
                setOf(),
            ) {
                GithubRunnerUserData(
                    runnerName = runnerName,
                    githubUrl = gitHub.url.toUrl(),
                    runnerToken = runnerToken,
                    runnerLabels = runtime.labels,
                    packages = runtime.packages,
                    runtime.allowSudo,
                    Distributor.ubuntu,

                ).toResult(context, defaultResources)
            }

            val server = HetznerServer(
                serverName,
                image = "ubuntu-24.04",
                userData = userData,
                location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
                sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud.environment))),
                volumes = emptySet(),
                type = cloud.hetznerProviderRuntime().defaultInstanceType,
                subnet = HetznerSubnetLookup(
                    defaultServiceSubnet,
                    HetznerNetworkLookup(networkName(cloud.environment)),
                ),
                privateIp = serverPrivateIp(runtime.index + it),
                labels = serviceLabels(runtime) + cloudLabels(cloud.environment) + Constants.indexLabels(it),
                dependsOn = defaultResources.list().toSet(),
                preApplyHook = { log ->
                    val results = runBlocking {
                        gitHub.listRunners().filter { it.name == runnerName }.map {
                            gitHub.deleteRunner(it, log)
                        }
                    }

                    if (results.hasError()) {
                        Error(results.aggregateErrors())
                    } else {
                        Success(Unit)
                    }
                },
            )

            defaultResources.list() + listOf(server)
        }

        return servers
    }

    private data class GithubContext(val api: GitHubApi, val url: GitHubUrlRuntime)

    private fun githubContext(cloud: CloudConfigurationRuntime): GithubContext {
        val githubUrl = cloud.githubProviderRuntime().githubUrl
        val githubApi = GitHubApi(cloud.githubProviderRuntime().githubToken)
        return GithubContext(githubApi, githubUrl)
    }

    override fun cleanupResources(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext, log: LogContext): Result<Unit> {
        val gitHub = githubContext(cloud)

        runBlocking {
            val runners = gitHub.listRunners()

            val serverNamePrefix = serverNamePrefix(cloud.environment, runtime.name)
            val serverNames = (0..<runtime.scale).map {
                serverName(cloud.environment, runtime.name, it)
            }

            val runnerNamePrefix = runtime.name
            val runnerNames = (0..<runtime.scale).map {
                "$runnerNamePrefix-$it"
            }

            val runnersForService = runners.filter { it.name.startsWith(runnerNamePrefix) }
            val runnersToDelete = runnersForService.filter { it.name !in runnerNames }

            val results = runnersToDelete.map { runner ->
                log.info("removing runner ${runner.name}")
                gitHub.deleteRunner(runner, log)
            }

            if (results.hasError()) {
                return@runBlocking Error<Unit>(results.aggregateErrors())
            }

            val servers = context.list<HetznerServerLookup, HetznerServerRuntime>(HetznerServerLookup::class).filter { it.name.startsWith(serverNamePrefix) }

            servers.filter { it.name.startsWith(serverNamePrefix) }.forEach { server ->
                if (server.name !in serverNames) {
                    log.info("removing ${server.logText()}")
                    context.destroy(HetznerServerLookup(server.name), log)
                }
            }
        }

        return Success(Unit)
    }

    override fun createProvisioners(runtime: GithubRunnerServiceConfigurationRuntime) = listOf<InfrastructureResourceProvisioner<*, *, *>>()

    override fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: GithubRunnerServiceConfiguration,
        context: ValidationContext,
        log: LogContext,
    ): Result<GithubRunnerServiceConfigurationRuntime> = Success(
        GithubRunnerServiceConfigurationRuntime(
            index,
            configuration.name,
            configuration.labels,
            configuration.packages,
            configuration.allowSudo,
            configuration.scale,
            InstanceRuntime.fromConfig(configuration.instance),
        ),
    )

    private suspend fun GithubContext.deleteRunner(runner: Runner, log: LogContext): Result<Unit> {
        log.info("removing runner ${runner.name}")

        when (val result = waitForBusyRunner(runner, log)) {
            is Error<Unit> -> return result
            is Success -> {}
        }

        when (this.url) {
            is Repository ->
                api.runners.deleteRepoRunner(url.username, url.repo, runner.id)

            is Organization -> {
                api.runners.deleteOrgRunner(url.org, runner.id)
            }
        }

        return Success(Unit)
    }

    private suspend fun GithubContext.waitForBusyRunner(runner: Runner, log: LogContext): Result<Unit> {
        log.info("removing runner ${runner.name}")

        val runner = when (this.url) {
            is Repository ->
                api.runners.getRepoRunner(url.username, url.repo, runner.id)

            is Organization -> {
                api.runners.getOrgRunner(url.org, runner.id)
            }
        }

        val result = VERY_LONG_WAIT.waitForCondition({
            log.info("runner '${runner.name}' still busy, waiting for job to complete.")
        }) {
            !runner.busy
        }

        return if (result) {
            Success(Unit)
        } else {
            Error("runner '${runner.name}' still busy")
        }
    }

    private suspend fun GithubContext.listRunners() = when (url) {
        is Repository ->
            api.runners.listRepoRunners(url.username, url.repo)

        is Organization -> {
            api.runners.listOrgRunners(url.org)
        }
    }

    private suspend fun GithubContext.createRegistrationToken() = when (url) {
        is Organization -> api.runners.createOrgRegistrationToken(url.org).token
        is Repository -> api.runners.createRepoRegistrationToken(url.username, url.repo).token
    }

    override val supportedConfiguration = GithubRunnerServiceConfiguration::class

    override val supportedRuntime = GithubRunnerServiceConfigurationRuntime::class
}
