package de.solidblocks.cloud.provisioner.hetzner.cloud

import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource
import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime
import de.solidblocks.hetzner.cloud.HetznerApi

open class BaseHetznerProvisioner(val hcloudToken: String) {
  val api = HetznerApi(hcloudToken)

  fun createLabelDiff(
      resource: LabeledInfrastructureResource<*, *>,
      runtime: LabeledInfrastructureResourceRuntime,
  ): List<ResourceDiffItem> {
    val missingLabels =
        resource.labels
            .filter { !runtime.labels.containsKey(it.key) }
            .map { ResourceDiffItem("label '${it.key}'", missing = true) }

    val changedLabels =
        resource.labels
            .map {
              if (runtime.labels.containsKey(it.key)) {
                if (runtime.labels.get(it.key) != it.value) {
                  ResourceDiffItem(
                      "label '${it.key}'",
                      changed = true,
                      expectedValue = it.value,
                      actualValue = runtime.labels.get(it.key),
                  )
                } else {
                  null
                }
              } else {
                null
              }
            }
            .filterNotNull()

    return missingLabels + changedLabels
  }
}
