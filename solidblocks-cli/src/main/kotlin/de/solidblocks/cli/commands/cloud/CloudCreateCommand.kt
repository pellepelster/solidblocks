package de.solidblocks.cli.commands.cloud

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.CloudReference
import de.solidblocks.cli.commands.BaseCloudDbCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import kotlin.system.exitProcess

class CloudCreateCommand :
    BaseCloudDbCommand(name = "create", help = "create a new cloud") {

    val cloud: String by option(help = "name of the cloud").required()

    val domain: String by option(help = "root domain for the cloud").required()

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        val reference = CloudReference(cloud)

        if (!context.verifyReference(reference)) {
            exitProcess(1)
        }

        context.cloudManager.createCloud(reference, domain)
    }
}
