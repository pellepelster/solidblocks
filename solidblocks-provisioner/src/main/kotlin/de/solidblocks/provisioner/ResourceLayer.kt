package de.solidblocks.provisioner

import de.solidblocks.core.IInfrastructureResource

data class ResourceLayer(val name: String, val resources: ArrayList<IInfrastructureResource<*>> = ArrayList()) {
    fun addResource(resource: IInfrastructureResource<*>) {
        this.resources.add(resource)
    }
}
