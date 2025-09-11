package de.solidblocks.cli.hetzner.asg

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int

class LoadBalancerReference(val reference: String)

class LocationReference(val reference: String)

class SSHKeyReference(val reference: String)

class ImageReference(val reference: String)

class FirewallReference(val reference: String)

class ServerTypeReference(val reference: String)

class PlacementGroupReference(val reference: String)

class HetznerAsgCommand : CliktCommand(name = "asg") {

    override fun help(context: Context) = "rolling rotate for servers attached to a load balancer"

    val loadbalancer by option(help = "id or name of the loadbalancer to attach new servers to").required()

    val location by option(help = "id or name of the location for new servers").default("nbg1")

    val serverType by option(help = "id or name of the server type for new servers").default("cx22")

    val image by option(help = "id or name of the image to use for new servers").default("debian-12")

    val placementGroup by option(help = "id or name of the placement group to use for new servers").default("debian-12")

    val enableIpv4 by option(help = "enable IpV4 for new servers").boolean().default(true)

    val enableIpv6 by option(help = "enable IpV6 for new servers").boolean().default(true)

    val sshKeys: List<String> by option(
        "--ssh-key",
        help = "id or name of the ssh key(s) to use for new servers"
    ).multiple()

    val firewall: List<String> by option(
        "--firewall",
        help = "id or name of the firewall(s) to use for new servers"
    ).multiple()

    val serverNamePrefix by option(help = "name prefix for the servers to create, if not provided the load balancer name will be used as prefix")

    val userData by option(help = "user data for newly created servers").file(
        mustExist = true, canBeFile = true, canBeDir = false
    ).required()

    val replicas by option(help = "number of replicas").int().default(1)

    private val hcloudToken by option(
        "--hcloud-token",
        help = "the api token for the project",
        envvar = "HCLOUD_TOKEN",
    ).required()


    override fun run() {
        HetznerAsg(hcloudToken).run(
            LoadBalancerReference(loadbalancer),
            LocationReference(location),
            ServerTypeReference(serverType),
            ImageReference(image),
            userData.readText(),
            replicas,
            serverNamePrefix,
            sshKeys.map { SSHKeyReference(it) },
            firewall.map { FirewallReference(it) },
            PlacementGroupReference(placementGroup),
            enableIpv4,
            enableIpv6,
        )
    }
}
