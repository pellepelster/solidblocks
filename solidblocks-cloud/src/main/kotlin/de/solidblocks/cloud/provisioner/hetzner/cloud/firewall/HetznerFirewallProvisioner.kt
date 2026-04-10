package de.solidblocks.cloud.provisioner.hetzner.cloud.firewall

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.resources.FirewallCreateRequest
import de.solidblocks.hetzner.cloud.resources.FirewallSetRulesRequest
import de.solidblocks.hetzner.cloud.resources.FirewallUpdateRequest
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

class HetznerFirewallProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerFirewallLookup, HetznerFirewallRuntime>,
    InfrastructureResourceProvisioner<HetznerFirewall, HetznerFirewallRuntime> {

    override suspend fun lookup(lookup: HetznerFirewallLookup, context: CloudProvisionerContext) = api.firewalls.get(lookup.name)?.let {
        HetznerFirewallRuntime(it.id, it.name, it.rules, it.labels)
    }

    override suspend fun diff(resource: HetznerFirewall, context: CloudProvisionerContext): ResourceDiff {
        val runtime = lookup(resource.asLookup(), context) ?: return ResourceDiff(resource, missing)

        val changes = mutableListOf<ResourceDiffItem>()

        if (resource.rules.toSet() != runtime.rules.toSet()) {
            changes.add(
                ResourceDiffItem(
                    "rules",
                    changed = true,
                    expectedValue = resource.rules.size,
                    actualValue = runtime.rules.size,
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

    override suspend fun apply(resource: HetznerFirewall, context: CloudProvisionerContext, log: LogContext): Result<HetznerFirewallRuntime> {
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

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error("error applying ${resource.logText()}")
    }

    override suspend fun destroy(resource: HetznerFirewall, context: CloudProvisionerContext, logContext: LogContext) = lookup(resource.asLookup(), context)?.let { api.firewalls.delete(it.id) } ?: false

    override val supportedLookupType: KClass<*> = HetznerFirewallLookup::class

    override val supportedResourceType: KClass<*> = HetznerFirewall::class
}
