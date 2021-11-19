package de.solidblocks.provisioner

import de.solidblocks.core.IInfrastructureResource

data class ResourceGroup(val name: String, val dependsOn: Set<ResourceGroup> = emptySet(), val resources: ArrayList<IInfrastructureResource<*, *>> = ArrayList()) {
    fun addResource(resource: IInfrastructureResource<*, *>) {
        this.resources.add(resource)
    }
}
