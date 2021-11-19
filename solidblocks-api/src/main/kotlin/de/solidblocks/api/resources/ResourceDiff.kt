package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.logName

class ResourceDiff<ReturnType>(
    val resource: IInfrastructureResource<ReturnType>,
    val missing: Boolean = false,
    private val error: Boolean = false,
    private val changes: List<ResourceDiffItem> = emptyList()
) {

    fun needsRecreate(): Boolean {
        return changes.any { it.triggersRecreate }
    }

    fun hasChanges(): Boolean {
        return missing || changes.any { it.hasChanges() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        if (missing) {
            return "${resource.logName()} is missing"
        }

        if (changes.isNotEmpty()) {
            return "${resource.logName()} has changes = ${
                changes.joinToString(", ")
            }"
        }


            return "${resource.logName()} is up to date"
    }
}
