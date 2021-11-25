package de.solidblocks.provisioner.consul.acl

import de.solidblocks.core.IInfrastructureResource

class ConsulAcl(val name: String) :
        IConsulAclLookup,
        IInfrastructureResource<ConsulAcl, ConsulAclRuntime> {

    override fun name(): String {
        return name
    }

}
