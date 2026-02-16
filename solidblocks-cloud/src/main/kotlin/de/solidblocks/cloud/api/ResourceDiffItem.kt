package de.solidblocks.cloud.api

import kotlin.reflect.KProperty0

fun ResourceDiffItem.logText(): String {
  if (expectedValue != null || actualValue != null) {
    return "$name should be '$expectedValue' but was '$actualValue"
  }

  if (missing) {
    return "$name is missing"
  }

  if (changed) {
    return "$name has changed"
  }

  return "$name"
}

data class ResourceDiffItem(
    val name: String,
    val changed: Boolean = false,
    val triggersRecreate: Boolean = false,
    val missing: Boolean = false,
    val expectedValue: Any? = null,
    val actualValue: Any? = null,
) {

  constructor(
      property: KProperty0<*>,
      triggersRecreate: Boolean = false,
      missing: Boolean = false,
      changed: Boolean = false,
      unknown: Boolean = false,
      expectedValue: Any? = null,
      actualValue: Any? = null,
  ) : this(
      property.name,
      triggersRecreate = triggersRecreate,
      missing = missing,
      changed = changed,
      expectedValue = expectedValue,
      actualValue = actualValue,
  )

  fun hasChanges(): Boolean = missing || changed
}
