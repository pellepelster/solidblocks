package de.solidblocks.cloud.provisioner.hetzner.cloud.firewall

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.joinToStringOrEmpty
import de.solidblocks.hetzner.cloud.model.HetznerApiErrorType
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.resources.FirewallApplyToResourcesRequest
import de.solidblocks.hetzner.cloud.resources.FirewallCreateRequest
import de.solidblocks.hetzner.cloud.resources.FirewallLabelSelector
import de.solidblocks.hetzner.cloud.resources.FirewallResource
import de.solidblocks.hetzner.cloud.resources.FirewallResourceType
import de.solidblocks.hetzner.cloud.resources.FirewallSetRulesRequest
import de.solidblocks.hetzner.cloud.resources.FirewallUpdateRequest
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

class HetznerFirewallProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerFirewallLookup, HetznerFirewallRuntime>,
    InfrastructureResourceProvisioner<HetznerFirewall, HetznerFirewallRuntime> {

    override suspend fun lookup(lookup: HetznerFirewallLookup, context: ProvisionerContext) = api.firewalls.get(lookup.name)?.let {
        val appliedToLabels = it.appliedTo.flatMap {
            it.labelSelector?.selector?.split(",")?.map {
                it.split("=").let {
                    if (it.count() != 2) {
                        throw RuntimeException("invalid label selector: $it")
                    }

                    Pair(it[0], it[1])
                }
            } ?: emptyList()
        }.toMap()

        HetznerFirewallRuntime(it.id, it.name, it.rules, it.labels, appliedToLabels)
    }

    override suspend fun diff(resource: HetznerFirewall, context: ProvisionerDiffContext): ResourceDiff {
        val runtime = lookup(resource.asLookup(), context) ?: return ResourceDiff(resource, missing)

        val changes = mutableListOf<ResourceDiffItem>()

        if (resource.rules.toSet() != runtime.rules.toSet()) {
            changes.add(
                ResourceDiffItem(
                    "rules",
                    changed = true,
                    expectedValue = resource.rules.joinToStringOrEmpty("; ") {
                        it.logText()
                    },
                    actualValue = runtime.rules.joinToStringOrEmpty("; ") {
                        it.logText()
                    },
                ),
            )
        }

        if (resource.appliedToLabels != runtime.appliedToLabels) {
            changes.add(
                ResourceDiffItem(
                    "label selectors",
                    changed = true,
                    expectedValue = resource.appliedToLabels.entries.joinToStringOrEmpty {
                        "${it.key}=${it.value}"
                    },
                    actualValue = runtime.appliedToLabels.entries.joinToStringOrEmpty {
                        "${it.key}=${it.value}"
                    },
                ),
            )
        }

        changes.addAll(createLabelDiff(resource, runtime))

        return if (changes.isEmpty()) {
            ResourceDiff(resource, up_to_date)
        } else {
            ResourceDiff(resource, has_changes, changes = changes)
        }
    }

    override suspend fun apply(resource: HetznerFirewall, context: ProvisionerApplyContext, log: LogContext): Result<HetznerFirewallRuntime> {
        val runtime = lookup(resource.asLookup(), context)

        if (runtime == null) {
            api.firewalls.create(
                FirewallCreateRequest(
                    name = resource.name,
                    rules = resource.rules,
                    labels = resource.labels,
                ),
            )
        } else {
            api.firewalls.update(runtime.id, FirewallUpdateRequest(labels = resource.labels))

            if (resource.rules.toSet() != runtime.rules.toSet()) {
                val actions = api.firewalls.setRules(runtime.id, FirewallSetRulesRequest(resource.rules))
                api.firewalls.waitForAction(actions)
            }
        }

        val fw = lookup(resource.asLookup(), context) ?: return Error("error applying ${resource.logText()}")

        if (resource.appliedToLabels.isNotEmpty()) {
            try {
                api.firewalls.applyToResources(
                    fw.id,
                    FirewallApplyToResourcesRequest(
                        listOf(
                            FirewallResource(FirewallResourceType.LABEL_SELECTOR, labelSelector = FirewallLabelSelector(resource.appliedToLabels.map { "${it.key}=${it.value}" }.joinToString(","))),
                        ),
                    ),
                )
            } catch (e: HetznerApiException) {
                if (e.error.code != HetznerApiErrorType.FIREWALL_ALREADY_APPLIED) {
                    return Error(e.error.message)
                }
            }
        }

        return Success(fw)
    }

    override suspend fun destroy(resource: HetznerFirewall, context: ProvisionerContext, log: LogContext) = lookup(resource.asLookup(), context)?.let { api.firewalls.delete(it.id) } ?: false

    override val supportedLookupType: KClass<*> = HetznerFirewallLookup::class

    override val supportedResourceType: KClass<*> = HetznerFirewall::class
}
