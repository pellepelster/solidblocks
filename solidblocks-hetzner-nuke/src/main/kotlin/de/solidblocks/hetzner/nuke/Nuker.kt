package de.solidblocks.hetzner.nuke

import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.general.Action
import me.tomsdevsn.hetznercloud.objects.request.ChangeProtectionRequest
import mu.KotlinLogging

private fun Action.successful(): Boolean = this.finished != null && this.status == "success"

private fun parseTags() {}

class Nuker(apiToken: String) {

  private val logger = KotlinLogging.logger {}

  private val hetznerCloudAPI = HetznerCloudAPI(apiToken)

  fun waitForAction(action: Action) =
      Waiter.defaultWaiter().waitFor {
        if (action.successful()) {
          return@waitFor true
        }

        val actionResult = hetznerCloudAPI.getAction(action.id)
        logger.info {
          "waiting for action '${actionResult.action.command}' to finish, current status is '${actionResult.action.status}'"
        }
        actionResult.action.successful()
      }

  fun deleteAll(doNuke: Boolean) {
    val planResults = deleteResources(false)

    if (planResults.isEmpty()) {
      logger.info { "no resources to delete" }
      return
    }

    if (doNuke) {
      logger.info { "deleting ${planResults.size} resources" }
      var count = 15
      while (count > 0) {
        logger.info { "waiting before starting delete $count seconds left..." }
        Thread.sleep(1000)
        count--
      }

      val deleteResults = deleteResources(doNuke)
      logger.info { "deleted a total of ${deleteResults.size} resources" }
    } else {
      logger.info { "would delete ${planResults.size} resources" }
    }
  }

  private fun deleteResources(doNuke: Boolean): ArrayList<Triple<String, String, Boolean>> {
    val results = ArrayList<Triple<String, String, Boolean>>()

    results.addAll(
        deleteResources(
            "volume",
            hetznerCloudAPI.volumes.volumes,
            { resource -> resource.id to resource.name },
            {
              if (it.protection.delete) {
                waitForAction(
                    hetznerCloudAPI
                        .changeVolumeProtection(
                            it.id,
                            ChangeProtectionRequest.builder().delete(false).build(),
                        )
                        .action,
                )
              }

              if (it.server != null) {
                waitForAction(hetznerCloudAPI.detachVolume(it.id).action)
              }
              hetznerCloudAPI.deleteVolume(it.id)
            },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "server",
            hetznerCloudAPI.servers.servers,
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deleteServer(it.id) },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "placement group",
            hetznerCloudAPI.getPlacementGroups().getPlacementGroups(),
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deletePlacementGroup(it.id) },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "floating ip",
            hetznerCloudAPI.floatingIPs.floatingIps,
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deleteFloatingIP(it.id) },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "load-balancer",
            hetznerCloudAPI.loadBalancers.loadBalancers,
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deleteLoadBalancer(it.id) },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "firewall",
            hetznerCloudAPI.firewalls.firewalls,
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deleteFirewall(it.id) },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "network",
            hetznerCloudAPI.networks.networks,
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deleteNetwork(it.id) },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "ssh key",
            hetznerCloudAPI.sshKeys.sshKeys,
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deleteSSHKey(it.id) },
            doNuke,
        ),
    )

    results.addAll(
        this.deleteResources(
            "certificate",
            hetznerCloudAPI.certificates.certificates,
            { resource -> resource.id to resource.name },
            { hetznerCloudAPI.deleteCertificate(it.id) },
            doNuke,
        ),
    )

    return results
  }

  private fun <T> deleteResources(
      resourceLogName: String,
      resources: List<T>,
      mapper: (resource: T) -> Pair<Long, String>,
      delete: (resource: T) -> Unit,
      doNuke: Boolean,
  ): List<Triple<String, String, Boolean>> {
    if (doNuke) {
      logger.info { "deleting all ${resourceLogName}s..." }
    } else {
      logger.info { "simulating deletion of all ${resourceLogName}s..." }
    }

    return resources.map {
      val resource = mapper.invoke(it)

      if (doNuke) {
        logger.info { "deleting $resourceLogName '${resource.second}'" }
        delete.invoke(it)
      } else {
        logger.info { "would delete $resourceLogName '${resource.second}'" }
      }

      Triple(resourceLogName, resource.second, true)
    }
  }
}
