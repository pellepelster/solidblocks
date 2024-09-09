package de.solidblocks.cli.hetzner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.utils.logError
import de.solidblocks.cli.utils.logInfo
import de.solidblocks.cli.utils.logWarning
import platform.posix.sleep

class NukeCommand : CliktCommand(name = "nuke", help = "delete all Hetzner cloud resources from a project") {
    private val hcloudToken by option(
        "--hcloud-token",
        help = "the api token for the project",
        envvar = "HCLOUD_TOKEN"
    ).required()

    private val doNuke by option(
        "--do-nuke",
        help = "actually delete resources, if not set only the resources that would be deleted are logged"
    ).flag(default = false)

    override fun run() {
        val nuker = HetznerNuker(hcloudToken)

        try {
            if (doNuke) {
                logError("nuking all resources, running simulation...")

                val result = nuker.simulate()
                if (result == 0) {
                    logInfo("no resources found to delete")
                    return
                }

                logInfo("will delete ${result} resources")

                var count = 15
                while (count > 0) {
                    logWarning("waiting before starting deletion, $count seconds left...")
                    sleep(1u)
                    count--
                }

                nuker.nuke()
            } else {
                logInfo("running a simulated nuke, add '--do-nuke' to actually delete resources")
                val result = nuker.simulate()
                logInfo("found ${result} resources to delete")
            }
        } catch (e: HetznerApiException) {
            logError("nuke failed error: ${e.error}")
        }
    }
}