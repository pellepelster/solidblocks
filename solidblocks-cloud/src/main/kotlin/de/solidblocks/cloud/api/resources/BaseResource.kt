package de.solidblocks.cloud.api.resources

abstract class BaseResource(val name: String, val dependsOn: Set<BaseResource>) {

    open fun logText(): String {
        var simpleName = this.javaClass.simpleName.removeSuffix("Lookup").lowercase()

        if (simpleName.isEmpty()) {
            simpleName = this.javaClass.superclass.simpleName.lowercase()
        }

        return "$simpleName '${this.name}'"
    }
}
