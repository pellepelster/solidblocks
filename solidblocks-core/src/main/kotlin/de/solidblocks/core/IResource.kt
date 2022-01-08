package de.solidblocks.core

@OptIn(ExperimentalStdlibApi::class)
fun IResource.logName(): String {
    var simpleName = this.javaClass.simpleName.lowercase()

    if (simpleName.isEmpty()) {
        simpleName = this.javaClass.superclass.simpleName.lowercase()
    }

    return "$simpleName resource '${this.name}'"
}

fun IResource.getInfraParents(): List<IInfrastructureResource<*, *>> {
    return this.parents.filterIsInstance<IInfrastructureResource<*, *>>()
}

fun IResource.getAllInfraParents(): List<IInfrastructureResource<*, *>> =
    getParentsInternal(this).filterIsInstance<IInfrastructureResource<*, *>>()

fun List<IResource>.getAllInfraParents(): List<IInfrastructureResource<*, *>> =
    this.flatMap { getParentsInternal(it).filterIsInstance<IInfrastructureResource<*, *>>() }

private fun getParentsInternal(resource: IResource): List<IResource> {
    val parents = mutableListOf<IResource>()
    resource.parents.forEach { getParentsInternal(it, parents) }

    return parents.toList()
}

private fun getParentsInternal(resource: IResource, parents: MutableList<IResource>) {
    parents.add(resource)
    resource.parents.forEach { getParentsInternal(it, parents) }
}

interface IResource {

    val parents: Set<IResource>
        get() = emptySet()

    val name: String
}
