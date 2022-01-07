package de.solidblocks.core

@OptIn(ExperimentalStdlibApi::class)
fun IResource.logName(): String {
    return "${this.javaClass.simpleName.lowercase()} resource '${this.id()}'"
}

fun IResource.getInfraParents(): List<IInfrastructureResource<*, *>> {
    return this.getParents().filterIsInstance<IInfrastructureResource<*, *>>()
}

fun IResource.getAllInfraParents(): List<IInfrastructureResource<*, *>> = getParentsInternal(this).filterIsInstance<IInfrastructureResource<*, *>>()

fun List<IResource>.getAllInfraParents(): List<IInfrastructureResource<*, *>> = this.flatMap { getParentsInternal(it).filterIsInstance<IInfrastructureResource<*, *>>() }

private fun getParentsInternal(resource: IResource): List<IResource> {
    val parents = mutableListOf<IResource>()
    resource.getParents().forEach { getParentsInternal(it, parents) }

    return parents.toList()
}

private fun getParentsInternal(resource: IResource, parents: MutableList<IResource>) {
    parents.add(resource)
    resource.getParents().forEach { getParentsInternal(it, parents) }
}

interface IResource {

    fun getParents(): Set<IResource> = emptySet()

    fun id(): String
}
