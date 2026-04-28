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
import de.solidblocks.cloud.providers.github.GitHubUrlRuntime
import de.solidblocks.cloud.providers.github.GitHubUrlRuntime.Organization
import de.solidblocks.cloud.providers.github.GitHubUrlRuntime.Repository
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ensureLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.toResult
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfiguration
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfigurationRuntime
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.markdown
import de.solidblocks.cloudinit.Distributor
import de.solidblocks.cloudinit.GithubRunnerUserData
import de.solidblocks.utils.LogContext
import kotlinx.coroutines.runBlocking

class GithubRunnerServiceManager : ServiceManager<GithubRunnerServiceConfiguration, GithubRunnerServiceConfigurationRuntime> {

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            (0..runtime.scale - 1).map {
                ServerInfo(sshConnectCommand(context, cloud, runtime, it))
            },
            emptyList(),
        ),
    )

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext): Result<String> = Success(
        markdown {
            h1("Service '${runtime.name}'")

            h2("Servers")
            (0..runtime.scale - 1).forEach {
                text("to access server **${serverName(cloud.environment, runtime.name, it)}** via SSH, run")
                listOf(codeBlock(sshConnectCommand(context, cloud, runtime, it)))
            }

            h2("Runner")
            text("GitHub URL: **${cloud.githubProviderRuntime().githubUrl}**")
            if (runtime.labels.isNotEmpty()) {
                text("Labels: **${runtime.labels}**")
            }
        },
    )

    override fun status(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext): Result<String> {
        /*
        val sshClient = context.createOrGetSshClient(serverName(cloud.environment, runtime.name))
        val result = sshClient.command("systemctl is-active ${runtime.name}.service")
        text(if (result.exitCode == 0) "active" else "inactive (${result.stdOut.trim()})")
         */

        return markdown {
        }.let { Success(it) }
    }

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext): List<BaseInfrastructureResource<*>> {
        val (githubUrl, githubApi, runnerToken) = getGithub(cloud)

        val servers = (0..runtime.scale - 1).flatMap {
            val runnerName = "${runtime.name}-$it"
            val serverName = serverName(cloud.environment, runtime.name, it)
            val defaultResources = this.createDefaultResources(cloud, runtime, it)

            val userData = UserData(
                setOf(defaultResources.dataVolume),
            ) {
                GithubRunnerUserData(
                    runnerName = runnerName,
                    githubUrl = githubUrl.toUrl(),
                    runnerToken = runnerToken,
                    runnerLabels = runtime.labels,
                    packages = runtime.packages,
                    runtime.allowSudo,
                    Distributor.ubuntu,
                    it.ensureLookup(defaultResources.dataVolume.asLookup()).device,

                ).toResult(context, defaultResources)
            }

            val server = HetznerServer(
                serverName,
                image = "ubuntu-24.04",
                userData = userData,
                location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
                sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud.environment))),
                volumes = setOf(defaultResources.dataVolume.asLookup()),
                type = cloud.hetznerProviderRuntime().defaultInstanceType,
                subnet = HetznerSubnetLookup(
                    defaultServiceSubnet,
                    HetznerNetworkLookup(networkName(cloud.environment)),
                ),
                privateIp = serverPrivateIp(runtime.index + it),
                labels = serviceLabels(runtime) + cloudLabels(cloud.environment) + Constants.indexLabels(it),
                dependsOn = defaultResources.list().toSet(),
                preApplyHook = { log ->
                    when (githubUrl) {
                        is Repository ->
                            runBlocking {
                                githubApi.runners.listRepoRunners(githubUrl.username, githubUrl.repo).filter { it.name == runnerName }.forEach {
                                    log.info("removing runner ${it.name}")
                                    githubApi.runners.deleteRepoRunner(githubUrl.username, githubUrl.repo, it.id)
                                }
                            }

                        is Organization -> runBlocking {
                            githubApi.runners.listOrgRunners(githubUrl.org).filter { it.name == runnerName }.forEach {
                                log.info("removing runner ${it.name}")
                                githubApi.runners.deleteOrgRunner(githubUrl.org, it.id)
                            }
                        }
                    }
                },
            )

            defaultResources.list() + listOf(server)
        }

        return servers
    }

    override fun cleanupResources(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext, log: LogContext): Result<Unit> {
        val (githubUrl, githubApi, runnerToken) = getGithub(cloud)

        runBlocking {
            val runners = when (githubUrl) {
                is Organization -> githubApi.runners.listOrgRunners(githubUrl.org)
                is Repository -> githubApi.runners.listRepoRunners(githubUrl.username, githubUrl.repo)
            }

            val serverNames = (0..runtime.scale - 1).map {
                serverName(cloud.environment, runtime.name, it)
            }

            val serverNamePrefix = serverNamePrefix(cloud.environment, runtime.name)

            val runnersForService = runners.filter { it.name.startsWith(serverNamePrefix) }
            val runnersToDelete = runnersForService.filter { it.name !in serverNames }

            runnersToDelete.forEach { runner ->
                log.info("removing runner ${runner.name}")
                when (githubUrl) {
                    is Repository ->
                        githubApi.runners.deleteRepoRunner(githubUrl.username, githubUrl.repo, runner.id)

                    is Organization ->
                        githubApi.runners.deleteOrgRunner(githubUrl.org, runner.id)
                }
            }

            val servers = context.list<HetznerServerRuntime>(HetznerServerLookup::class).filter { it.name.startsWith(serverNamePrefix) }

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
        context: ProvisionerContext,
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

    override val supportedConfiguration = GithubRunnerServiceConfiguration::class

    override val supportedRuntime = GithubRunnerServiceConfigurationRuntime::class
}

private fun getGithub(cloud: CloudConfigurationRuntime): Triple<GitHubUrlRuntime, GitHubApi, String> {
    val githubUrl = cloud.githubProviderRuntime().githubUrl
    val githubApi = GitHubApi(cloud.githubProviderRuntime().githubToken)

    val runnerToken = runBlocking {
        when (githubUrl) {
            is Organization -> githubApi.runners.createOrgRegistrationToken(githubUrl.org).token
            is Repository -> githubApi.runners.createRepoRegistrationToken(githubUrl.username, githubUrl.repo).token
        }
    }
    return Triple(githubUrl, githubApi, runnerToken)
}
