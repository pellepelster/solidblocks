package de.solidblocks.cloudinit

import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CurlLibrary
import de.solidblocks.shell.DockerLibrary
import de.solidblocks.shell.GithubLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.UserLibrary
import de.solidblocks.shell.systemd.Install
import de.solidblocks.shell.systemd.Restart
import de.solidblocks.shell.systemd.Service
import de.solidblocks.shell.systemd.SystemDService
import de.solidblocks.shell.systemd.Target
import de.solidblocks.shell.systemd.Unit
import de.solidblocks.shell.systemd.installSystemDUnit
import de.solidblocks.shell.systemd.startSystemDUnit

class GithubRunnerUserData(
    val runnerName: String,
    val githubUrl: String,
    val runnerToken: String,
    val runnerLabels: List<String>,
) : ServiceUserData {

    private val runnerUser = "github-runner"
    private val runnerHome = "/home/$runnerUser"
    private val runnerDir1 = "$runnerHome"

    override fun shellScript(): ShellScript {
        val shellScript = ShellScript()

        shellScript.addLibrary(AptLibrary)
        shellScript.addLibrary(CurlLibrary)
        shellScript.addLibrary(GithubLibrary)
        shellScript.addCommand(AptLibrary.UpdateRepositories())
        shellScript.addCommand(AptLibrary.UpdateSystem())
        shellScript.addCommand(AptLibrary.InstallPackage("curl"))
        shellScript.addCommand(AptLibrary.InstallPackage("ca-certificates"))
        shellScript.addLibrary(DockerLibrary)
        shellScript.addCommand(DockerLibrary.InstallDebian())
        shellScript.addCommand(GithubLibrary.InstallRunner())
        shellScript.addCommand(UserLibrary.AddUserToGroup("github-runner", "docker"))

        val systemDService = SystemDService(
            runnerName,
            Unit("Github actions runner '$runnerName'", listOf(Target.NETWORK_ONLINE_TARGET), listOf(Target.NETWORK_ONLINE_TARGET)),
            Service(
                listOf("$runnerDir1/start_runner.sh"),
                restart = Restart.ALWAYS,
                user = runnerUser,
                group = runnerUser,
                workingDirectory = runnerDir1,
                environment = mapOf(
                    "GITHUB_URL" to githubUrl,
                    "RUNNER_TOKEN" to runnerToken,
                    "RUNNER_NAME" to runnerName,
                    "RUNNER_LABELS" to runnerLabels.joinToString(","),

                ),
            ),
            Install(Target.MULTI_USER_TARGET),
        )

        shellScript.installSystemDUnit(systemDService)
        shellScript.startSystemDUnit(systemDService)

        return shellScript
    }
}
