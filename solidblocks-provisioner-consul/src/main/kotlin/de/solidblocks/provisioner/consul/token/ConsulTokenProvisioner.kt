package de.solidblocks.provisioner.consul.token

import com.orbitz.consul.Consul
import com.orbitz.consul.ConsulException
import com.orbitz.consul.model.acl.ImmutablePolicyLink
import com.orbitz.consul.model.acl.ImmutableToken
import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import mu.KotlinLogging
import java.util.*

class ConsulTokenProvisioner(val consul: Consul) :
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

        val currentToken = lookup(resource)

        if (!currentToken.isEmpty()) {
            return Result<Any>(failed = false)
        }

        val policies = resource.policies.map { ImmutablePolicyLink.builder().name(it.id).build() }
        val token = ImmutableToken.builder().addAllPolicies(policies).description(resource.description).id(resource.id.toString())
        consul.aclClient().createToken(token.build())

        return Result<ConsulToken>(failed = false)
    }

    override fun diff(resource: ConsulToken): Result<ResourceDiff> {
        return Result(
            failed = false,
            result = ResourceDiff(resource = resource, missing = true)
        )
    }

    override fun lookup(lookup: IConsulTokenLookup): Result<ConsulTokenRuntime> {
        try {
            val token = consul.aclClient().readToken(lookup.id())
                ?: return Result.emptyResult()
            return Result.resultOf(ConsulTokenRuntime(UUID.fromString(token.accessorId()), token.secretId()))
        } catch (e: ConsulException) {
        }

        return Result.failedResult()
    }
}
