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

    override fun getResourceType(): Class<ConsulKv> {
        return ConsulKv::class.java
    }

    override fun getLookupType(): Class<*> {
        return IConsulKvLookup::class.java
    }

    override fun apply(resource: ConsulKv): Result<*> {
        consul.keyValueClient().putValue(resource.key)
        return Result<ConsulKv>(failed = false)
    }

    override fun diff(resource: ConsulKv): Result<ResourceDiff> {
        val keys = consul.keyValueClient().getKeys(resource.key)

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
