package de.solidblocks.cli.hetzner.asg

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import de.solidblocks.cli.utils.logInfo

class LoadBalancerReference(val reference: String)

class LocationReference(val reference: String)

class SSHKeyReference(val reference: String)

class ImageReference(val reference: String)

class FirewallReference(val reference: String)

class NetworkReference(val reference: String)

class ServerTypeReference(val reference: String)

class PlacementGroupReference(val reference: String)

class HetznerAsgCommand : CliktCommand(name = "asg") {

    override fun help(context: Context) = "Manage and update servers attached to Hetzner load balancers"

    override fun run() {
    }

}

class HetznerAsgRotateCommand : CliktCommand(name = "rotate") {

    init {
        context {
            helpFormatter = {
                MordantMarkdownHelpFormatter(
                    it,
                    showDefaultValues = true,
                    showRequiredTag = true,
                    requiredOptionMarker = "*"
                )
            }
        }
    }

    override fun help(context: Context) = """
        ## Overview
        
        A rolling replace for cloud servers attached to a Hetzner load balancer. All servers that are 
        attached to a Hetzner load balancer provided by `--loadbalancer` will be replaced with new 
        servers created using the user data from `--user-data`.
        
        ## Details
        
        The command works stateless in the sense that all needed information is stored as meta-data
        in the labels of the affected Hetzner cloud resources. Only resources created by this command
        are managed, the meta-data label `blcks.de/managed-by` set to **asg** serves as marker for those
        resources.
        
        The decision if a server needs to be replaced is made by comparing the SHA-256 hash of 
        the user data script used to create a server with the hash of the new user data script 
        provided by `--user-data`. The user data hash is stored in the `blcks.de/user-data-hash`
        label of each server.
        
        The rotate process is atomic and can be cancelled and restarted at any time. Once 
        started it will try to reconcile towards the target state of *n* up-to-date `--replicas`
        and afterward remove all old servers attached to the `--loadbalancer`.
         
        If a server gets detached from a load balancer, it will automatically be re-attached. 
        To achieve this, the load balancer association is stored in the 
        `blcks.de/load-balancer-id` labels.
    """.trimIndent()

    val loadbalancer by option(help = "id or name of the loadbalancer to attach new servers to").required()

    val location by option(help = "id or name of the location for new servers").default("nbg1")

    val serverType by option(help = "id or name of the server type for new servers").default("cx22")

    val image by option(help = "id or name of the image to use for new servers").default("debian-12")

    val placementGroup by option(help = "id or name of the placement group to use for new servers")

    val enableIpv4 by option(help = "enable IpV4 for new servers").boolean().default(true)

    val enableIpv6 by option(help = "enable IpV6 for new servers").boolean().default(true)

    val sshKeys: List<String> by option(
        "--ssh-key",
        help = "id or name of the ssh key(s) to use for new servers"
    ).multiple()

    val firewalls: List<String> by option(
        "--firewall",
        help = "id or name of the firewall(s) to use for new servers"
    ).multiple()

    val networks: List<String> by option(
        "--network",
        help = "id or name of the networks(s) to use for new servers"
    ).multiple()

    val serverNamePrefix by option(help = "name prefix for the servers to create, if not provided the load balancer name will be used as prefix")

    val userData by option(help = "user data for newly created servers").file(
        mustExist = true, canBeFile = true, canBeDir = false
    ).required()

    val replicas by option(help = "number of replicas").int().default(1)

    private val hcloudToken by option(
        "--hcloud-token",
        help = "the api token for the project, can also be provided via the environment variable *HCLOUD_TOKEN*",
        envvar = "HCLOUD_TOKEN",
    ).required()


    override fun run() {
        val result = HetznerAsg(hcloudToken).rotate(
            LoadBalancerReference(loadbalancer),
            LocationReference(location),
            ServerTypeReference(serverType),
            ImageReference(image),
            userData.readText(),
            replicas,
            serverNamePrefix,
            sshKeys.map { SSHKeyReference(it) },
            firewalls.map { FirewallReference(it) },
            networks.map { NetworkReference(it) },
            placementGroup?.let { PlacementGroupReference(it) },
            enableIpv4,
            enableIpv6,
        )

        when (result) {
            ASG_ROTATE_STATUS.OK -> logInfo("rollout finished")
            ASG_ROTATE_STATUS.TIMEOUT -> throw CliktError("rollout did not finish within timeout")
            ASG_ROTATE_STATUS.LOADBALANCER_NOT_FOUND -> throw CliktError("loadbalancer '${loadbalancer}' not found")
        }
    }
}
