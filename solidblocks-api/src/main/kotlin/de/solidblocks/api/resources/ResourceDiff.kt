package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource

class ResourceDiff<ReturnType>(
    val resource: IInfrastructureResource<ReturnType>,
    val missing: Boolean = false,
    val unknown: Boolean = false,
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
        return "missing = $missing, unknown = $unknown, error = $error,  changes = ${
        changes.joinToString(", ")
        }"
    }
}
