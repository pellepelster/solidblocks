package de.solidblocks.provisioner.consul.acl

import com.ecwid.consul.v1.acl.model.NewAcl
import com.ecwid.consul.v1.agent.model.NewService
import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.consul.provider.ConsulClientWrapper
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class ConsulAclProvisioner(val consulClient: ConsulClientWrapper) :
        IResourceLookupProvider<IConsulAclLookup, ConsulAclRuntime>,
        IInfrastructureResourceProvisioner<ConsulAcl, ConsulAclRuntime> {

    private val logger = KotlinLogging.logger {}


    override fun getResourceType(): Class<ConsulAcl> {
        return ConsulAcl::class.java
    }

    override fun getLookupType(): Class<*> {
        return IConsulAclLookup::class.java
    }

    override fun apply(resource: ConsulAcl): Result<*> {
        val acl = NewAcl()
        acl.name = resource.name
        consulClient.execute { consul, token ->
            consul.aclCreate(acl, token)
        }

        return Result<ConsulAcl>(resource = resource, failed = false)
    }

    override fun diff(resource: ConsulAcl): Result<ResourceDiff> {
        val result = consulClient.execute { consul, token ->
            consul.getAclList(token)
        }

        val missing = result.value.none { it.name == resource.name }
        return Result(
                failed = false,
                resource = resource,
                result = ResourceDiff(resource = resource, missing = missing)
        )
    }

    override fun lookup(lookup: IConsulAclLookup): Result<ConsulAclRuntime> {
        TODO("Not yet implemented")
    }
}
