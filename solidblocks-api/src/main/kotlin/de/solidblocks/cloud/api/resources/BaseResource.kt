package de.solidblocks.cloud.api.resources

abstract class BaseResource(val name: String, val dependsOn: Set<BaseResource>) {

    open fun logText(): String {
        var simpleName = this.javaClass.simpleName.removeSuffix("Lookup").lowercase()

        if (simpleName.isEmpty()) {
            simpleName = this.javaClass.superclass.simpleName.lowercase()
        }

        return "$simpleName '${this.name}'"
    }

    fun recursiveDependsOn(): MutableSet<BaseResource> {
        val allDependsOn = mutableSetOf<BaseResource>()
        collectDependsOn(this, allDependsOn)
        return allDependsOn
    }

    private fun collectDependsOn(resource: BaseResource, dependsOn: MutableSet<BaseResource>) {
        resource.dependsOn.forEach {
            // only recurse into newly seen dependencies, otherwise cyclic or diamond
            // dependency graphs would loop forever / be re-traversed exponentially
            if (dependsOn.add(it)) {
                collectDependsOn(it, dependsOn)
            }
        }
    }
}
