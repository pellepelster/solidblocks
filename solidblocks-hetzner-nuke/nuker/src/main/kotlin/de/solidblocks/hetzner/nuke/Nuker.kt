package de.solidblocks.hetzner.nuke

import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.general.Action
import me.tomsdevsn.hetznercloud.objects.general.Certificate
import mu.KotlinLogging

private fun Action.succesfull(): Boolean = this.finished != null && this.status == "success"


class Nuker(apiToken: String) {

    private val logger = KotlinLogging.logger {}

    private val hetznerCloudAPI = HetznerCloudAPI(apiToken)

    fun waitForAction(action: Action) = Waiter.defaultWaiter().waitFor {

        if (action.succesfull()) {
            return@waitFor true
        }

        val actionResult = hetznerCloudAPI.getAction(action.id)
        logger.info { "waiting for action '${actionResult.action.command}' to finish, current status is '${actionResult.action.status}'" }
        actionResult.action.succesfull()
    }

    fun deleteAll(doNuke: Boolean) {

        if (doNuke) {
            var count = 15
            while (count > 0) {
                logger.info { "waiting before starting delete ${count} seconds left..." }
                Thread.sleep(1000)
                count--
            }
        }

        this.deleteResources(
            "volume",
            hetznerCloudAPI.volumes.volumes,
            { resource -> resource.id to resource.name },
            {
                waitForAction(hetznerCloudAPI.detachVolume(it).action)
                hetznerCloudAPI.deleteVolume(it)
            },
            doNuke
        )

        this.deleteResources(
            "server",
            hetznerCloudAPI.servers.servers,
            { resource -> resource.id to resource.name },
            hetznerCloudAPI::deleteServer,
            doNuke
        )

        this.deleteResources(
            "floating ip",
            hetznerCloudAPI.floatingIPs.floatingIps,
            { resource -> resource.id to resource.name },
            hetznerCloudAPI::deleteFloatingIP,
            doNuke
        )

        this.deleteResources(
            "load-balancer",
            hetznerCloudAPI.loadBalancers.loadBalancers,
            { resource -> resource.id to resource.name },
            hetznerCloudAPI::deleteLoadBalancer,
            doNuke
        )


        this.deleteResources(
            "firewall",
            hetznerCloudAPI.firewalls.firewalls,
            { resource -> resource.id to resource.name },
            hetznerCloudAPI::deleteFirewall,
            doNuke
        )

        this.deleteResources(
            "network",
            hetznerCloudAPI.networks.networks,
            { resource -> resource.id to resource.name },
            hetznerCloudAPI::deleteNetwork,
            doNuke
        )

        this.deleteResources(
            "ssh key",
            hetznerCloudAPI.sshKeys.sshKeys,
            { resource -> resource.id to resource.name },
            hetznerCloudAPI::deleteSSHKey,
            doNuke
        )

        this.deleteResources(
            "certificate",
            hetznerCloudAPI.certificates.certificates,
            { resource -> resource.id to resource.name },
            hetznerCloudAPI::deleteCertificate,
            doNuke
        )
    }


    private fun <T> deleteResources(
        resourceLogName: String,
        resources: List<T>,
        mapper: (resource: T) -> Pair<Long, String>,
        delete: (id: Long) -> Unit,
        doNuke: Boolean
    ) {
        if (doNuke) {
            logger.info { "deleting all ${resourceLogName}s..." }
        } else {
            logger.info { "simulating deletion of all ${resourceLogName}s..." }
        }

        resources.forEach {
            val resource = mapper.invoke(it)
            if (doNuke) {
                logger.info { "deleting ${resourceLogName} '${resource.second}'" }
                delete.invoke(resource.first)
            } else {
                logger.info { "would delete ${resourceLogName} '${resource.second}'" }
            }
        }
    }
}

