package de.solidblocks.provisioner.consul.token

import com.orbitz.consul.Consul
import com.orbitz.consul.model.acl.ImmutablePolicyLink
import com.orbitz.consul.model.acl.ImmutableToken
import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.*

@Component
class ConsulTokenProvisioner(val consulClient: Consul) :
    IResourceLookupProvider<IConsulTokenLookup, ConsulTokenRuntime>,
    IInfrastructureResourceProvisioner<ConsulToken, ConsulTokenRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun getResourceType(): Class<ConsulToken> {
        return ConsulToken::class.java
    }

    override fun getLookupType(): Class<*> {
        return IConsulTokenLookup::class.java
    }

    override fun apply(resource: ConsulToken): Result<*> {
        val policies = resource.policies.map { ImmutablePolicyLink.builder().name(it.id).build() }
        val token = ImmutableToken.builder().addAllPolicies(policies).description(resource.description).id(resource.id.toString())
        consulClient.aclClient().createToken(token.build())

        return Result<ConsulToken>(resource = resource, failed = false)
    }

    override fun diff(resource: ConsulToken): Result<ResourceDiff> {
        return Result(
            failed = false,
            resource = resource,
            result = ResourceDiff(resource = resource, missing = true)
        )
    }

    override fun lookup(lookup: IConsulTokenLookup): Result<ConsulTokenRuntime> {
        val token = consulClient.aclClient().readToken(lookup.id().toString()) ?: return Result.emptyResult()
        return Result.of(ConsulTokenRuntime(UUID.fromString(token.accessorId())))
    }
}
