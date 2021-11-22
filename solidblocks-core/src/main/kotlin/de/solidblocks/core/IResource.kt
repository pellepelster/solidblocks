package de.solidblocks.core

@OptIn(ExperimentalStdlibApi::class)
fun IResource.logName(): String {
    return "${this.javaClass.simpleName.lowercase()} resource '${this.name()}'"
}

fun IResource.getInfraParents(): List<IInfrastructureResource<*, *>> {
    return this.getParents().filterIsInstance<IInfrastructureResource<*, *>>()
}


interface IResource {

    fun getParents(): List<IResource> = emptyList()

    fun name(): String
}