package de.solidblocks.provisioner.consul.policy

import com.orbitz.consul.Consul
import com.orbitz.consul.model.acl.ImmutablePolicy
import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import mu.KotlinLogging

class ConsulPolicyProvisioner(val consul: Consul) :
    IResourceLookupProvider<IConsulPolicyLookup, ConsulPolicyRuntime>,
    IInfrastructureResourceProvisioner<ConsulPolicy, ConsulPolicyRuntime> {

    private val logger = KotlinLogging.logger {}

    override val resourceType = ConsulPolicy::class.java

    override val lookupType = IConsulPolicyLookup::class.java

    override fun apply(resource: ConsulPolicy): Result<*> {

        val currentPolicy = lookup(resource)
        if (!currentPolicy.isEmpty()) {
            return Result<Any>(failed = false)
        }

        val policy = ImmutablePolicy.builder().name(resource.name).rules(resource.rules)
        consul.aclClient().createPolicy(policy.build())

        return Result<ConsulPolicy>(failed = false)
    }

    override fun diff(resource: ConsulPolicy): Result<ResourceDiff> {
        val policies = consul.aclClient().listPolicies()

        val policy = policies.firstOrNull { it.name() == resource.name }
            ?: return Result(
                failed = false,
                result = ResourceDiff(resource = resource, missing = true)
            )

        // rules currently not available in api
        return Result(
            failed = false,
            result = ResourceDiff(resource = resource, changes = listOf(ResourceDiffItem(name = "rules", changed = true)))
        )
    }

    override fun lookup(lookup: IConsulPolicyLookup): Result<ConsulPolicyRuntime> {
        val policies = consul.aclClient().listPolicies()

        val policy = policies.firstOrNull { it.name() == lookup.name }
        if (policy == null) {
            return Result(failed = false)
        }

        return Result(ConsulPolicyRuntime(policy.id()))
    }
}
