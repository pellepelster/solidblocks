package de.solidblocks.cloud.services

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.firewallName
import de.solidblocks.cloud.Constants.serviceLabels
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.firewall.HetznerFirewall
import de.solidblocks.hetzner.cloud.resources.FirewallRuleDirection
import de.solidblocks.hetzner.cloud.resources.FirewallRuleProtocol
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallRule

interface ServiceConfigurationRuntime {
    val index: Int
    val name: String
}


fun ServiceConfigurationRuntime.firewall(cloud: CloudConfigurationRuntime, ports: List<Int>) = HetznerFirewall(
    firewallName(cloud, this.name), ports.map {
        HetznerFirewallRule(
            direction = FirewallRuleDirection.IN,
            protocol = FirewallRuleProtocol.TCP,
            port = "${it}",
            sourceIps = listOf("0.0.0.0/0", "::/0"),
            description = "HTTP",
        )
    }, cloudLabels(cloud),
    serviceLabels(this)
)
