package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.logText

fun List<ResourceDiff>.logText() =
    "\n${" ".repeat(4)}changed resources: ${
    this.filter { it.hasChanges() }.joinToString("") { "\n${" ".repeat(8)}- ${it.resource.logText()}" }.ifEmpty { "<none>" }
    }\n${" ".repeat(4)}missing resources: ${
    this.filter { it.missing }.joinToString("") { "\n${" ".repeat(8)}- ${it.resource.logText()}" }.ifEmpty { "<none>" }
    }\n${" ".repeat(4)}unknown resources: ${
    this.filter { it.unknown }.joinToString("") { "\n${" ".repeat(8)}- ${it.resource.logText()}" }.ifEmpty { "<none>" }
    }"

class ResourceDiff(
    val resource: IInfrastructureResource<*, *>,
    val missing: Boolean = false,
    val unknown: Boolean = false,
    private val changes: List<ResourceDiffItem> = emptyList()
) {

    fun needsRecreate(): Boolean {
        return changes.any { it.triggersRecreate }
    }

    fun needsApply(): Boolean {
        return isMissing() || hasChanges() || unknown
    }

    fun isMissing(): Boolean {
        return missing
    }

    fun hasChanges(): Boolean {
        return changes.any { it.hasChanges() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        if (missing) {
            return "${resource.logText()} is missing"
        }

        if (changes.isNotEmpty()) {
            return "${resource.logText()} has changes = ${
            changes.joinToString(", ")
            }"
        }

        return "${resource.logText()} is up to date"
    }
}
