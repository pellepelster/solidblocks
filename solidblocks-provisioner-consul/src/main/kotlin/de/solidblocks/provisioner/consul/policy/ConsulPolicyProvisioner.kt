package de.solidblocks.provisioner.consul.policy

import com.orbitz.consul.Consul
import com.orbitz.consul.model.acl.ImmutablePolicy
import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class ConsulPolicyProvisioner(val consulClient: Consul) :
    IResourceLookupProvider<IConsulPolicyLookup, ConsulPolicyRuntime>,
    IInfrastructureResourceProvisioner<ConsulPolicy, ConsulPolicyRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun getResourceType(): Class<ConsulPolicy> {
        return ConsulPolicy::class.java
    }

    override fun getLookupType(): Class<*> {
        return IConsulPolicyLookup::class.java
    }

    override fun apply(resource: ConsulPolicy): Result<*> {
        val policy = ImmutablePolicy.builder().name(resource.id).rules(resource.rules)
        consulClient.aclClient().createPolicy(policy.build())

        return Result<ConsulPolicy>(resource = resource, failed = false)
    }

    override fun diff(resource: ConsulPolicy): Result<ResourceDiff> {
        val policies = consulClient.aclClient().listPolicies()

        val policy = policies.firstOrNull { it.name() == resource.id }
            ?: return Result(
                failed = false,
                resource = resource,
                result = ResourceDiff(resource = resource, missing = true)
            )

        // rules currently not available in api
        return Result(
            failed = false,
            resource = resource,
            result = ResourceDiff(resource = resource, changes = listOf(ResourceDiffItem(name = "rules", changed = true)))
        )
    }

    override fun lookup(lookup: IConsulPolicyLookup): Result<ConsulPolicyRuntime> {
        TODO("Not yet implemented")
    }
}
