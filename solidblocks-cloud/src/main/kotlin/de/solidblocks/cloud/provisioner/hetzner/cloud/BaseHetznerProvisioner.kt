package de.solidblocks.cloud.provisioner.hetzner.cloud

import de.solidblocks.cloud.Constants
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime
import de.solidblocks.hetzner.cloud.HetznerApi

open class BaseHetznerProvisioner(val hcloudToken: String) {
    val api = HetznerApi(hcloudToken)

    fun createLabelDiff(
        resource: BaseLabeledInfrastructureResource<*>,
        runtime: BaseLabeledInfrastructureResourceRuntime,
        ignoredLabelPrefixes: List<String> = listOf("${Constants.namespace}/ssh-keys", "${Constants.namespace}/user-data")
    ): List<ResourceDiffItem> {

        val resourceLabels = resource.labels.filter { ignoredLabelPrefixes.none { prefix -> it.key.startsWith(prefix) } }
        val runtimeLabels = runtime.labels.filter { ignoredLabelPrefixes.none { prefix -> it.key.startsWith(prefix) } }

        val missingLabels =
            resourceLabels
                .filter { !runtimeLabels.containsKey(it.key) }
                .map { ResourceDiffItem("label '${it.key}'", missing = true) }

        val deletedLabels =
            runtimeLabels
                .filter { !resourceLabels.containsKey(it.key) }
                .map { ResourceDiffItem("label '${it.key}'", changed = true) }

        val changedLabels =
            resourceLabels
                .map {
                    if (runtimeLabels.containsKey(it.key)) {
                        if (runtimeLabels.get(it.key) != it.value) {
                            ResourceDiffItem(
                                "label '${it.key}'",
                                changed = true,
                                expectedValue = it.value,
                                actualValue = runtimeLabels.get(it.key),
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                .filterNotNull()

        return missingLabels + changedLabels + deletedLabels
    }
}
