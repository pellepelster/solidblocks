package de.solidblocks.provisioner

import de.solidblocks.core.IInfrastructureResource

data class ResourceGroup(val name: String, val dependsOn: Set<ResourceGroup> = emptySet(), val resources: ArrayList<IInfrastructureResource<*, *>> = ArrayList()) {

    fun <T : IInfrastructureResource<*, *>> addResource(resource: T): T {
        this.resources.add(resource)

        return resource
    }
}
