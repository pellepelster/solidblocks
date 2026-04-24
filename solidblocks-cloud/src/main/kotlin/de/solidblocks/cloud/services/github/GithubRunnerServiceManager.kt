package de.solidblocks.cloud.services.github

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.defaultServiceSubnet
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.serverPrivateIp
import de.solidblocks.cloud.Constants.serviceLabels
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.github.GitHubApi
import de.solidblocks.cloud.providers.github.GitHubUrlRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ensureLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfiguration
import de.solidblocks.cloud.services.github.model.GithubRunnerServiceConfigurationRuntime
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.markdown
import de.solidblocks.cloudinit.GithubRunnerUserData
import de.solidblocks.shell.toCloudInit
import de.solidblocks.utils.LogContext
import kotlinx.coroutines.runBlocking

class GithubRunnerServiceManager : ServiceManager<GithubRunnerServiceConfiguration, GithubRunnerServiceConfigurationRuntime> {

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            listOf(ServerInfo(sshConnectCommand(context, cloud, runtime))),
            emptyList(),
        ),
    )

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext): Result<String> = Success(
        markdown {
            h1("Service '${runtime.name}'")

            h2("Servers")
            text("to access server **${serverName(cloud.environment, runtime.name)}** via SSH, run")
            codeBlock(sshConnectCommand(context, cloud, runtime))

            h2("Runner")
            text("GitHub URL: **${cloud.githubProviderRuntime().githubUrl}**")
            if (runtime.labels.isNotEmpty()) {
                text("Labels: **${runtime.labels}**")
            }
        },
    )

    override fun status(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext): Result<String> {
        val sshClient = context.createOrGetSshClient(serverName(cloud.environment, runtime.name))
        val result = sshClient.command("systemctl is-active ${runtime.name}.service")

        return markdown {
            h1("Service '${runtime.name}'")
            h2("Runner Status")
            text(if (result.exitCode == 0) "active" else "inactive (${result.stdOut.trim()})")
        }.let { Success(it) }
    }

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: GithubRunnerServiceConfigurationRuntime, context: ProvisionerContext): List<BaseInfrastructureResource<*>> {
        val runnerName = "${runtime.name}-0"

        val githubUrl = cloud.githubProviderRuntime().githubUrl
        val githubApi = GitHubApi(cloud.githubProviderRuntime().githubToken)

        val serverName = serverName(cloud.environment, runtime.name)
        val defaultResources = this.createDefaultResources(cloud, runtime)

        val userData = UserData(
            setOf(defaultResources.dataVolume),
        ) {
            val runnerToken = runBlocking {
                when (githubUrl) {
                    is GitHubUrlRuntime.Organization -> githubApi.runners.createOrgRegistrationToken(githubUrl.org).token
                    is GitHubUrlRuntime.Repository -> githubApi.runners.createRepoRegistrationToken(githubUrl.username, githubUrl.repo).token
                }
            }

            GithubRunnerUserData(
                runnerName = runnerName,
                githubUrl = githubUrl.toUrl(),
                runnerToken = runnerToken,
                runnerLabels = runtime.labels,
            ).shellScript().toCloudInit(
                it.ensureLookup(defaultResources.sshIdentityRsaSecret.asLookup()).secret,
                it.ensureLookup(defaultResources.sshIdentityED25519Secret.asLookup()).secret,
            ).render()
        }

        val server = HetznerServer(
            serverName,
            userData = userData,
            location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud.environment))),
            volumes = setOf(defaultResources.dataVolume.asLookup()),
            type = cloud.hetznerProviderRuntime().defaultInstanceType,
            subnet = HetznerSubnetLookup(
                defaultServiceSubnet,
                HetznerNetworkLookup(networkName(cloud.environment)),
            ),
            privateIp = serverPrivateIp(runtime.index),
            labels = serviceLabels(runtime) + cloudLabels(cloud.environment),
            dependsOn = defaultResources.list().toSet(),
            preApplyHook = { log ->
                when (githubUrl) {
                    is GitHubUrlRuntime.Repository ->
                        runBlocking {
                            githubApi.runners.listRepoRunners(githubUrl.username, githubUrl.repo).filter { it.name == runnerName }.forEach {
                                log.info("removing runner ${it.name}")
                                githubApi.runners.deleteRepoRunner(githubUrl.username, githubUrl.repo, it.id)
                            }
                        }

                    is GitHubUrlRuntime.Organization -> runBlocking {
                        githubApi.runners.listOrgRunners(githubUrl.org).filter { it.name == runnerName }.forEach {
                            log.info("removing runner ${it.name}")
                            githubApi.runners.deleteOrgRunner(githubUrl.org, it.id)
                        }
                    }
                }
            },
        )

        return listOf(server) + defaultResources.list()
    }

    override fun createProvisioners(runtime: GithubRunnerServiceConfigurationRuntime) = listOf<InfrastructureResourceProvisioner<*, *>>()

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
            listOf(configuration.labels), // TODO
            InstanceRuntime.fromConfig(configuration.instance),
        ),
    )

    override val supportedConfiguration = GithubRunnerServiceConfiguration::class

    override val supportedRuntime = GithubRunnerServiceConfigurationRuntime::class
}
