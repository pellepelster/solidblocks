package de.solidblocks.core

@OptIn(ExperimentalStdlibApi::class)
fun IResource.logName(): String {
    return "${this.javaClass.simpleName.lowercase()} resource '${this.name()}'"
}

interface IResource {
    fun name(): String
}
