package de.solidblocks.provisioner.consul.kv

import com.orbitz.consul.Consul
import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import mu.KotlinLogging

public class ConsulKvProvisioner(val consul: Consul) :
    IResourceLookupProvider<IConsulKvLookup, ConsulKvRuntime>,
    IInfrastructureResourceProvisioner<ConsulKv, ConsulKvRuntime> {

    private val logger = KotlinLogging.logger {}

    override val resourceType = ConsulKv::class.java

    override val lookupType = IConsulKvLookup::class.java

    override fun apply(resource: ConsulKv): Result<*> {
        consul.keyValueClient().putValue(resource.name)
        return Result<ConsulKv>(failed = false)
    }

    override fun diff(resource: ConsulKv): Result<ResourceDiff> {
        val keys = consul.keyValueClient().getKeys(resource.name)

        if (keys.isEmpty()) {
            return Result.resultOf(ResourceDiff(resource = resource, missing = true))
        }

        return Result(
            result = ResourceDiff(
                resource = resource
            )
        )
    }

    override fun lookup(lookup: IConsulKvLookup): Result<ConsulKvRuntime> {
        TODO("Not yet implemented")
    }
}
