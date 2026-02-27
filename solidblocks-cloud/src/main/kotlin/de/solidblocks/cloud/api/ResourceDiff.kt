package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import java.io.StringWriter

fun List<ResourceDiff>.logText(): String {
  val sw = StringWriter()
  sw.append("\n")
  sw.appendLine("${" ".repeat(4)}changed resources:")

  for (resourceDiff in this.filter { it.hasChanges() }) {
    sw.appendLine("${" ".repeat(8)}- ${resourceDiff.resource.logText()}")
    for (diffItem in resourceDiff.changes) {
      sw.appendLine("${" ".repeat(12)}- ${diffItem.logText()}")
    }
  }

  sw.appendLine("${" ".repeat(4)}missing resources:")
  for (resourceDiff in this.filter { it.status == missing }) {
    sw.appendLine("${" ".repeat(8)}- ${resourceDiff.resource.logText()}")
    for (diffItem in resourceDiff.changes) {
      sw.appendLine("${" ".repeat(12)}- ${diffItem.logText()}")
    }
  }

  return sw.toString()
}

enum class ResourceDiffStatus {
  missing,
  parent_missing,
  up_to_date,
  unknown,
  has_changes,
  duplicate,
}

class ResourceDiff(
    val resource: BaseInfrastructureResource<*>,
    val status: ResourceDiffStatus,
    val duplicateErrorMessage: String? = null,
    val changes: List<ResourceDiffItem> = emptyList(),
) {

  fun needsRecreate(): Boolean = changes.any { it.triggersRecreate }

  fun hasChanges(): Boolean = changes.any { it.hasChanges() }

  override fun toString(): String {
    when (status) {
      ResourceDiffStatus.missing -> return "${resource.logText()} is missing"
      else -> {
        if (changes.isNotEmpty()) {
          return "${resource.logText()} has changes = ${
                        changes.joinToString(", ")
                    }"
        }

        return "${resource.logText()} is up to date"
      }
    }
  }
}
