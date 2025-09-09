package de.solidblocks.cli.hetzner.asg

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long

sealed class LoadBalancerReference {
    data class LoadBalancerId(val id: Long) : LoadBalancerReference()
    data class LoadBalancerName(val name: String) : LoadBalancerReference()
}

class ServerOptions : OptionGroup() {
    val serverNamePrefix by option(help = "name prefix for the servers to create, if no provided the load balancer name will be used as prefix")
}

class HetznerAsgCommand : CliktCommand(name = "asg") {

    override fun help(context: Context) = "rolling rotate for servers attached to a load balancer"

    val loadbalancer: LoadBalancerReference by mutuallyExclusiveOptions<LoadBalancerReference>(
        option("--lb-id", help = "id of the loadbalancer").long().convert { LoadBalancerReference.LoadBalancerId(it) },
        option(
            "--lb-name",
            help = "name of the loadbalancer"
        ).convert { LoadBalancerReference.LoadBalancerName(it) }
    ).required()

    val server by ServerOptions()

    private val hcloudToken by
    option(
        "--hcloud-token",
        help = "the api token for the project",
        envvar = "HCLOUD_TOKEN",
    )
        .required()


    override fun run() {
        HetznerAsg(hcloudToken).run(loadbalancer)
    }
}
